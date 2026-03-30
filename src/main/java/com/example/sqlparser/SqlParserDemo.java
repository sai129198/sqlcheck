package com.example.sqlparser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.parser.SqlParser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 解析器示例程序
 */
public class SqlParserDemo {

    public static void main(String[] args) {
        SqlParser parser = new SqlParser();

        System.out.println("========== SQL Parser Demo ==========\n");

        // 示例 1: 简单 SELECT
        demoSimpleSelect(parser);

        // 示例 2: 带 JOIN 的 SELECT
        demoSelectWithJoin(parser);

        // 示例 3: INSERT ... SELECT
        demoInsertSelect(parser);

        // 示例 4: 带表达式的 SELECT（血缘分析）
        demoExpressionLineage(parser);

        // 示例 5: CTE
        demoCte(parser);

        System.out.println("\n========== Demo Complete ==========");
    }

    private static void demoSimpleSelect(SqlParser parser) {
        System.out.println("--- Example 1: Simple SELECT ---");
        String sql = "SELECT id, name, email FROM users WHERE status = 'active'";
        System.out.println("SQL: " + sql);

        SqlStatement stmt = parser.parse(sql);
        System.out.println("Type: " + stmt.getType());

        List<TableRef> tables = stmt.getAllTables();
        System.out.println("Tables: " + tables.stream().map(TableRef::getName).collect(Collectors.toList()));

        List<ColumnLineage> lineage = stmt.getAllColumnLineage();
        System.out.println("Columns: " + lineage.stream().map(ColumnLineage::getTargetColumn).collect(Collectors.toList()));
        System.out.println();
    }

    private static void demoSelectWithJoin(SqlParser parser) {
        System.out.println("--- Example 2: SELECT with JOIN ---");
        String sql = "SELECT u.name, o.order_date FROM users u JOIN orders o ON u.id = o.user_id";
        System.out.println("SQL: " + sql);

        SqlStatement stmt = parser.parse(sql);

        List<TableRef> tables = stmt.getAllTables();
        System.out.println("Tables:");
        for (TableRef table : tables) {
            System.out.println("  - " + table.getName() + (table.getAlias() != null ? " (alias: " + table.getAlias() + ")" : ""));
        }

        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        System.out.println("Joins: " + query.getFrom().getJoins().size());
        System.out.println();
    }

    private static void demoInsertSelect(SqlParser parser) {
        System.out.println("--- Example 3: INSERT ... SELECT ---");
        String sql = "INSERT INTO target_users (id, name) SELECT user_id, user_name FROM source_users WHERE active = 1";
        System.out.println("SQL: " + sql);

        SqlStatement stmt = parser.parse(sql);
        System.out.println("Type: " + stmt.getType());

        InsertDetails details = stmt.getInsertDetails();
        System.out.println("Target Table: " + details.getTargetTable().getTableName());
        System.out.println("Target Columns: " + details.getTargetColumns());
        System.out.println("Mode: " + details.getMode());

        System.out.println("Column Mappings:");
        for (ColumnMapping mapping : details.getColumnMappings()) {
            System.out.println("  - " + mapping.getTargetColumn() + " <- " + 
                mapping.getSourceLineage().getOriginalExpression());
        }
        System.out.println();
    }

    private static void demoExpressionLineage(SqlParser parser) {
        System.out.println("--- Example 4: Expression Lineage ---");
        String sql = "SELECT id, price * quantity AS total, CONCAT(first_name, ' ', last_name) AS full_name FROM orders";
        System.out.println("SQL: " + sql);

        SqlStatement stmt = parser.parse(sql);
        SelectClause select = stmt.getSelectDetails().getQueryBlock().getSelect();

        System.out.println("Column Lineage:");
        for (ColumnRef col : select.getColumns()) {
            System.out.println("  Column: " + col.getName());
            if (col.getLineage() != null) {
                System.out.println("    Expression Type: " + col.getLineage().getExpressionType());
                System.out.println("    Original Expression: " + col.getLineage().getOriginalExpression());
                System.out.println("    Sources: " + col.getLineage().getSources().size());
            }
        }
        System.out.println();
    }

    private static void demoCte(SqlParser parser) {
        System.out.println("--- Example 5: CTE (WITH clause) ---");
        String sql = "WITH regional_sales AS (SELECT region, SUM(amount) AS total FROM orders GROUP BY region) " +
                     "SELECT * FROM regional_sales WHERE total > 1000";
        System.out.println("SQL: " + sql);

        SqlStatement stmt = parser.parse(sql);
        System.out.println("CTEs: " + stmt.getCtes().size());

        for (CteDefinition cte : stmt.getCtes()) {
            System.out.println("  - " + cte.getName());
        }

        List<TableRef> tables = stmt.getAllTables();
        System.out.println("Main Query Tables: " + tables.stream().map(TableRef::getName).collect(Collectors.toList()));
        System.out.println();
    }
}
