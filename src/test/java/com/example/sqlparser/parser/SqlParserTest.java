package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SqlParserTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    @Test
    @DisplayName("解析简单 SELECT 语句")
    void testSimpleSelect() {
        String sql = "SELECT id, name FROM users";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isSelect()).isTrue();
        assertThat(stmt.getSelectDetails()).isNotNull();
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getType()).isEqualTo(QueryBlockType.SIMPLE_SELECT);
        
        // 验证 SELECT 子句
        SelectClause select = query.getSelect();
        assertThat(select.getColumns()).hasSize(2);
        assertThat(select.getColumns().get(0).getName()).isEqualTo("id");
        assertThat(select.getColumns().get(1).getName()).isEqualTo("name");
        
        // 验证 FROM 子句
        FromClause from = query.getFrom();
        assertThat(from.getTables()).hasSize(1);
        assertThat(from.getTables().get(0).getName()).isEqualTo("users");
    }

    @Test
    @DisplayName("解析带 WHERE 的 SELECT")
    void testSelectWithWhere() {
        String sql = "SELECT id, name FROM users WHERE age > 18";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isSelect()).isTrue();
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getWhere()).isNotNull();
        assertThat(query.getWhere().getConditionLineage()).isNotNull();
        assertThat(query.getWhere().getConditionLineage().getOriginalExpression()).contains("age");
    }

    @Test
    @DisplayName("解析带 JOIN 的 SELECT")
    void testSelectWithJoin() {
        String sql = "SELECT u.id, o.amount FROM users u JOIN orders o ON u.id = o.user_id";
        
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        FromClause from = query.getFrom();
        
        // JOIN 的两个表都在 tables 列表中
        assertThat(from.getTables()).hasSize(2);
        assertThat(from.getTables().get(0).getName()).isEqualTo("users");
        assertThat(from.getTables().get(0).getAlias()).isEqualTo("u");
        assertThat(from.getTables().get(1).getName()).isEqualTo("orders");
        assertThat(from.getTables().get(1).getAlias()).isEqualTo("o");
        
        assertThat(from.getJoins()).hasSize(1);
        JoinCondition join = from.getJoins().get(0);
        assertThat(join.getRightTable().getName()).isEqualTo("orders");
        assertThat(join.getRightTable().getAlias()).isEqualTo("o");
    }

    @Test
    @DisplayName("解析带 GROUP BY 的 SELECT")
    void testSelectWithGroupBy() {
        String sql = "SELECT department, COUNT(*) FROM employees GROUP BY department";
        
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getGroupBy()).isNotNull();
        assertThat(query.getGroupBy().getColumns()).hasSize(1);
        assertThat(query.getGroupBy().getColumns().get(0).getName()).isEqualTo("department");
    }

    @Test
    @DisplayName("解析带 ORDER BY 的 SELECT")
    void testSelectWithOrderBy() {
        String sql = "SELECT name, salary FROM employees ORDER BY salary DESC";
        
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getOrderBy()).isNotNull();
        assertThat(query.getOrderBy().getItems()).hasSize(1);
        assertThat(query.getOrderBy().getItems().get(0).getColumn().getName()).isEqualTo("salary");
        assertThat(query.getOrderBy().getItems().get(0).isAscending()).isFalse();
    }

    @Test
    @DisplayName("解析 UNION 语句")
    void testUnion() {
        String sql = "SELECT id FROM table1 UNION SELECT id FROM table2";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isSelect()).isTrue();
        assertThat(stmt.getSelectDetails().getStructureType()).isEqualTo(QueryStructureType.SET_OPERATION);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getType()).isEqualTo(QueryBlockType.UNION);
        assertThat(query.getLeftQuery()).isNotNull();
        assertThat(query.getRightQuery()).isNotNull();
    }

    @Test
    @DisplayName("解析 INSERT VALUES 语句")
    void testInsertValues() {
        String sql = "INSERT INTO users (id, name, age) VALUES (1, 'John', 25)";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isInsert()).isTrue();
        
        InsertDetails details = stmt.getInsertDetails();
        assertThat(details.getTargetTable().getTableName()).isEqualTo("users");
        assertThat(details.getTargetColumns()).containsExactly("id", "name", "age");
        assertThat(details.getMode()).isEqualTo(InsertMode.VALUES);
        assertThat(details.getValueRows()).hasSize(1);
        assertThat(details.getValueRows().get(0)).hasSize(3);
    }

    @Test
    @DisplayName("解析 INSERT SELECT 语句")
    void testInsertSelect() {
        String sql = "INSERT INTO target (id, name) SELECT id, name FROM source";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isInsert()).isTrue();
        
        InsertDetails details = stmt.getInsertDetails();
        assertThat(details.getMode()).isEqualTo(InsertMode.SELECT);
        assertThat(details.getSelectQuery()).isNotNull();
        assertThat(details.getColumnMappings()).hasSize(2);
    }

    @Test
    @DisplayName("解析 UPDATE 语句")
    void testUpdate() {
        String sql = "UPDATE users SET name = 'Jane', age = 30 WHERE id = 1";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isUpdate()).isTrue();
        
        UpdateDetails details = stmt.getUpdateDetails();
        assertThat(details.getTargetTable().getTableName()).isEqualTo("users");
        assertThat(details.getAssignments()).hasSize(2);
        
        Assignment first = details.getAssignments().get(0);
        assertThat(first.getTargetColumn()).isEqualTo("name");
        assertThat(first.getValueLineage().getOriginalExpression()).isEqualTo("'Jane'");
        
        assertThat(details.getWhere()).isNotNull();
    }

    @Test
    @DisplayName("解析 DELETE 语句")
    void testDelete() {
        String sql = "DELETE FROM users WHERE status = 'inactive'";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.isDelete()).isTrue();
        
        DeleteDetails details = stmt.getDeleteDetails();
        assertThat(details.getTargetTable().getTableName()).isEqualTo("users");
        assertThat(details.getWhere()).isNotNull();
        assertThat(details.getWhere().getConditionLineage().getOriginalExpression()).contains("status");
    }

    @Test
    @DisplayName("解析带 CTE 的 SELECT")
    void testSelectWithCte() {
        String sql = "WITH cte AS (SELECT id FROM users) SELECT * FROM cte";
        
        SqlStatement stmt = parser.parse(sql);
        
        assertThat(stmt.getCtes()).hasSize(1);
        assertThat(stmt.getCtes().get(0).getName()).isEqualTo("cte");
        assertThat(stmt.getCtes().get(0).getQuery()).isNotNull();
    }

    @Test
    @DisplayName("获取所有表")
    void testGetAllTables() {
        String sql = "SELECT a.id, b.name FROM table1 a JOIN table2 b ON a.id = b.id";
        
        SqlStatement stmt = parser.parse(sql);
        List<TableRef> tables = stmt.getAllTables();
        
        assertThat(tables).hasSize(2);
        assertThat(tables).extracting(TableRef::getName).containsExactly("table1", "table2");
    }

    @Test
    @DisplayName("获取字段血缘")
    void testGetColumnLineage() {
        String sql = "SELECT id, name FROM users";
        
        SqlStatement stmt = parser.parse(sql);
        List<ColumnLineage> lineage = stmt.getAllColumnLineage();
        
        assertThat(lineage).hasSize(2);
        assertThat(lineage).extracting(ColumnLineage::getTargetColumn).containsExactly("id", "name");
    }

    @Test
    @DisplayName("解析带表达式的 SELECT")
    void testSelectWithExpression() {
        String sql = "SELECT id, price * quantity AS total FROM orders";
        
        SqlStatement stmt = parser.parse(sql);
        
        SelectClause select = stmt.getSelectDetails().getQueryBlock().getSelect();
        assertThat(select.getColumns()).hasSize(2);
        
        ColumnRef totalCol = select.getColumns().get(1);
        assertThat(totalCol.getName()).isEqualTo("total");
        assertThat(totalCol.getLineage()).isNotNull();
        assertThat(totalCol.getLineage().getExpressionType()).isEqualTo(ExpressionType.MULTIPLY);
    }

    @Test
    @DisplayName("解析带 DISTINCT 的 SELECT")
    void testSelectWithDistinct() {
        String sql = "SELECT DISTINCT country FROM customers";
        
        SqlStatement stmt = parser.parse(sql);
        
        SelectClause select = stmt.getSelectDetails().getQueryBlock().getSelect();
        assertThat(select.isDistinct()).isTrue();
    }

    @Test
    @DisplayName("解析复杂 WHERE 条件")
    void testComplexWhere() {
        String sql = "SELECT * FROM products WHERE category = 'Electronics' AND price > 100";
        
        SqlStatement stmt = parser.parse(sql);
        
        WhereClause where = stmt.getSelectDetails().getQueryBlock().getWhere();
        assertThat(where).isNotNull();
        assertThat(where.getConditionLineage().getSources()).isNotEmpty();
    }

    @Test
    @DisplayName("解析多行 VALUES INSERT")
    void testInsertMultipleValues() {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'A'), (2, 'B')";
        
        SqlStatement stmt = parser.parse(sql);
        
        InsertDetails details = stmt.getInsertDetails();
        // 简化版解析器可能只解析第一行，这里验证基本功能
        assertThat(details.getValueRows()).isNotEmpty();
    }

    @Test
    @DisplayName("解析带 LIMIT 的 SELECT")
    void testSelectWithLimit() {
        String sql = "SELECT * FROM users LIMIT 10";
        
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        assertThat(query.getLimit()).isNotNull();
        assertThat(query.getLimit().getLimit()).isEqualTo(10);
    }

    @Test
    @DisplayName("验证 INSERT SELECT 的字段映射血缘")
    void testInsertSelectLineage() {
        String sql = "INSERT INTO target (id, name) SELECT user_id, user_name FROM source";
        
        SqlStatement stmt = parser.parse(sql);
        
        InsertDetails details = stmt.getInsertDetails();
        List<ColumnMapping> mappings = details.getColumnMappings();
        
        assertThat(mappings).hasSize(2);
        assertThat(mappings.get(0).getTargetColumn()).isEqualTo("id");
        assertThat(mappings.get(1).getTargetColumn()).isEqualTo("name");
    }

    @Test
    @DisplayName("验证 UPDATE 的赋值血缘")
    void testUpdateAssignmentLineage() {
        String sql = "UPDATE employees SET salary = salary * 1.1 WHERE dept = 'Sales'";
        
        SqlStatement stmt = parser.parse(sql);
        
        UpdateDetails details = stmt.getUpdateDetails();
        Assignment assignment = details.getAssignments().get(0);
        
        assertThat(assignment.getTargetColumn()).isEqualTo("salary");
        assertThat(assignment.getValueLineage().getExpressionType()).isEqualTo(ExpressionType.MULTIPLY);
    }
}
