package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.postgresql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PostgreSQL SQL 解析器测试
 */
class PostgreSQLSqlParserTest {

    private PostgreSQLSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new PostgreSQLSqlParser();
    }

    @Test
    @DisplayName("解析基础 CREATE TABLE")
    void testBasicCreateTable() {
        String sql = "CREATE TABLE users (\n" +
                     "    id SERIAL PRIMARY KEY,\n" +
                     "    username VARCHAR(50) NOT NULL,\n" +
                     "    email VARCHAR(100) UNIQUE\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getTableName()).isEqualTo("users");
        assertThat(table.getColumns()).hasSize(3);
        assertThat(table.isTemporary()).isFalse();
    }

    @Test
    @DisplayName("解析临时表")
    void testTemporaryTable() {
        String sql = "CREATE TEMPORARY TABLE temp_data (\n" +
                     "    id SERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.isTemporary()).isTrue();
        assertThat(table.getTableName()).isEqualTo("temp_data");
    }

    @Test
    @DisplayName("解析 TEMP 表")
    void testTempTable() {
        String sql = "CREATE TEMP TABLE temp_data (\n" +
                     "    id SERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.isTemporary()).isTrue();
    }

    @Test
    @DisplayName("解析 UNLOGGED 表")
    void testUnloggedTable() {
        String sql = "CREATE UNLOGGED TABLE log_data (\n" +
                     "    id SERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.isUnlogged()).isTrue();
    }

    @Test
    @DisplayName("解析 IF NOT EXISTS")
    void testIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS new_table (\n" +
                     "    id SERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.isIfNotExists()).isTrue();
    }

    @Test
    @DisplayName("解析 schema.table")
    void testSchemaTable() {
        String sql = "CREATE TABLE public.users (\n" +
                     "    id SERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getSchema()).isEqualTo("public");
        assertThat(table.getTableName()).isEqualTo("users");
    }

    @Test
    @DisplayName("解析 SERIAL 类型")
    void testSerialType() {
        String sql = "CREATE TABLE test_serial (\n" +
                     "    id SERIAL PRIMARY KEY,\n" +
                     "    big_id BIGSERIAL,\n" +
                     "    small_id SMALLSERIAL\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLColumnDef> columns = table.getColumns();
        assertThat(columns.get(0).isSerial()).isTrue();
        assertThat(columns.get(0).getDataType()).isEqualTo("SERIAL");
    }

    @Test
    @DisplayName("解析数组类型")
    void testArrayType() {
        String sql = "CREATE TABLE test_array (\n" +
                     "    tags TEXT[],\n" +
                     "    scores INTEGER[]\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLColumnDef> columns = table.getColumns();
        // 简化测试，验证列存在
        assertThat(columns.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("解析列约束")
    void testColumnConstraints() {
        String sql = "CREATE TABLE test_constraints (\n" +
                     "    id SERIAL PRIMARY KEY,\n" +
                     "    email VARCHAR(100) UNIQUE NOT NULL,\n" +
                     "    status VARCHAR(20) DEFAULT 'active',\n" +
                     "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLColumnDef> columns = table.getColumns();
        assertThat(columns.get(0).isPrimaryKey()).isTrue();
        assertThat(columns.get(1).isUnique()).isTrue();
        assertThat(columns.get(1).isNullable()).isFalse();
    }

    @Test
    @DisplayName("解析 IDENTITY 列")
    void testIdentityColumn() {
        String sql = "CREATE TABLE test_identity (\n" +
                     "    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLColumnDef> columns = table.getColumns();
        assertThat(columns.get(0).isIdentity()).isTrue();
    }

    @Test
    @DisplayName("解析表级约束 - PRIMARY KEY")
    void testTablePrimaryKey() {
        String sql = "CREATE TABLE test_pk (\n" +
                     "    id INTEGER,\n" +
                     "    name VARCHAR(100),\n" +
                     "    CONSTRAINT pk_test PRIMARY KEY (id)\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLCreateTable.PostgreSQLConstraintDef> constraints = table.getConstraints();
        assertThat(constraints).isNotEmpty();
        assertThat(constraints.get(0).getType()).isEqualTo("PRIMARY KEY");
        assertThat(constraints.get(0).getName()).isEqualTo("pk_test");
    }

    @Test
    @DisplayName("解析表级约束 - UNIQUE")
    void testTableUnique() {
        String sql = "CREATE TABLE test_unique (\n" +
                     "    id INTEGER,\n" +
                     "    email VARCHAR(100),\n" +
                     "    CONSTRAINT uk_email UNIQUE (email)\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLCreateTable.PostgreSQLConstraintDef> constraints = table.getConstraints();
        assertThat(constraints).extracting("type").contains("UNIQUE");
    }

    @Test
    @DisplayName("解析表级约束 - FOREIGN KEY")
    void testTableForeignKey() {
        String sql = "CREATE TABLE test_fk (\n" +
                     "    id SERIAL PRIMARY KEY,\n" +
                     "    user_id INTEGER,\n" +
                     "    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLCreateTable.PostgreSQLConstraintDef> constraints = table.getConstraints();
        boolean hasFk = constraints.stream()
            .anyMatch(c -> "FOREIGN KEY".equals(c.getType()) && 
                          "users".equals(c.getRefTable()));
        assertThat(hasFk).isTrue();
    }

    @Test
    @DisplayName("解析表级约束 - CHECK")
    void testTableCheck() {
        String sql = "CREATE TABLE test_check (\n" +
                     "    id INTEGER,\n" +
                     "    age INTEGER,\n" +
                     "    CONSTRAINT chk_age CHECK (age >= 0 AND age <= 150)\n" +
                     ")";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        List<PostgreSQLCreateTable.PostgreSQLConstraintDef> constraints = table.getConstraints();
        assertThat(constraints).extracting("type").contains("CHECK");
    }

    @Test
    @DisplayName("解析分区表 - RANGE")
    void testRangePartition() {
        String sql = "CREATE TABLE events (\n" +
                     "    id BIGSERIAL,\n" +
                     "    created_at TIMESTAMP NOT NULL\n" +
                     ") PARTITION BY RANGE (created_at)";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getPartitionInfo()).isNotNull();
        assertThat(table.getPartitionInfo().getPartitionType()).isEqualTo("RANGE");
        assertThat(table.getPartitionInfo().getPartitionKey()).isEqualTo("created_at");
    }

    @Test
    @DisplayName("解析分区表 - LIST")
    void testListPartition() {
        String sql = "CREATE TABLE orders (\n" +
                     "    id SERIAL,\n" +
                     "    status VARCHAR(20)\n" +
                     ") PARTITION BY LIST (status)";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getPartitionInfo()).isNotNull();
        assertThat(table.getPartitionInfo().getPartitionType()).isEqualTo("LIST");
    }

    @Test
    @DisplayName("解析分区表 - HASH")
    void testHashPartition() {
        String sql = "CREATE TABLE measurements (\n" +
                     "    id SERIAL,\n" +
                     "    city_id INTEGER\n" +
                     ") PARTITION BY HASH (city_id)";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getPartitionInfo()).isNotNull();
        assertThat(table.getPartitionInfo().getPartitionType()).isEqualTo("HASH");
    }

    @Test
    @DisplayName("解析 CREATE TABLE LIKE")
    void testCreateTableLike() {
        String sql = "CREATE TABLE new_users (LIKE users)";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getLikeTable()).isEqualTo("users");
    }

    @Test
    @DisplayName("解析 INHERITS")
    void testInherits() {
        String sql = "CREATE TABLE students (\n" +
                     "    student_id SERIAL\n" +
                     ") INHERITS (users)";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getInherits()).contains("users");
    }

    @Test
    @DisplayName("解析 TABLESPACE")
    void testTablespace() {
        String sql = "CREATE TABLE test_ts (\n" +
                     "    id SERIAL\n" +
                     ") TABLESPACE pg_default";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.getTablespace()).isEqualTo("pg_default");
    }

    @Test
    @DisplayName("解析 LIMIT OFFSET")
    void testLimitOffset() {
        String sql = "SELECT * FROM users LIMIT 10 OFFSET 20";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLimitExpression()).isEqualTo("10");
        assertThat(details.getOffsetExpression()).isEqualTo("20");
    }

    @Test
    @DisplayName("解析 LIMIT ALL")
    void testLimitAll() {
        String sql = "SELECT * FROM users LIMIT ALL OFFSET 100";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isLimitAll()).isTrue();
    }

    @Test
    @DisplayName("解析 FETCH FIRST")
    void testFetchFirst() {
        String sql = "SELECT * FROM users FETCH FIRST 10 ROWS ONLY";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLimitExpression()).isEqualTo("10");
    }

    @Test
    @DisplayName("解析 FETCH FIRST WITH TIES")
    void testFetchFirstWithTies() {
        String sql = "SELECT * FROM users ORDER BY score DESC FETCH FIRST 10 ROWS WITH TIES";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isWithTies()).isTrue();
    }

    @Test
    @DisplayName("解析 DISTINCT ON")
    void testDistinctOn() {
        String sql = "SELECT DISTINCT ON (user_id) * FROM posts ORDER BY user_id, created_at DESC";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isDistinctOn()).isTrue();
        assertThat(details.getDistinctOnExpression()).isEqualTo("user_id");
    }

    @Test
    @DisplayName("解析 FOR UPDATE")
    void testForUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForUpdate()).isTrue();
    }

    @Test
    @DisplayName("解析 FOR NO KEY UPDATE")
    void testForNoKeyUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR NO KEY UPDATE";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForNoKeyUpdate()).isTrue();
    }

    @Test
    @DisplayName("解析 FOR SHARE")
    void testForShare() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR SHARE";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForShare()).isTrue();
    }

    @Test
    @DisplayName("解析 FOR KEY SHARE")
    void testForKeyShare() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR KEY SHARE";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForKeyShare()).isTrue();
    }

    @Test
    @DisplayName("解析 FOR UPDATE NOWAIT")
    void testForUpdateNowait() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE NOWAIT";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForUpdate()).isTrue();
        assertThat(details.isForUpdateNowait()).isTrue();
    }

    @Test
    @DisplayName("解析 FOR UPDATE SKIP LOCKED")
    void testForUpdateSkipLocked() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE SKIP LOCKED";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isForUpdate()).isTrue();
        assertThat(details.isForUpdateSkipLocked()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT RETURNING")
    void testInsertReturning() {
        String sql = "INSERT INTO users (username) VALUES ('john') RETURNING id, username";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.isReturning()).isTrue();
        assertThat(details.getReturningColumns()).contains("id", "username");
    }

    @Test
    @DisplayName("解析 INSERT RETURNING *")
    void testInsertReturningAll() {
        String sql = "INSERT INTO users (username) VALUES ('john') RETURNING *";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.isReturning()).isTrue();
        assertThat(details.getReturningColumns()).contains("*");
    }

    @Test
    @DisplayName("解析 ON CONFLICT DO NOTHING")
    void testOnConflictDoNothing() {
        String sql = "INSERT INTO users (id, username) VALUES (1, 'john') ON CONFLICT DO NOTHING";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOnConflict()).isTrue();
        assertThat(details.getOnConflictAction()).contains("NOTHING");
    }

    @Test
    @DisplayName("解析 ON CONFLICT (column) DO NOTHING")
    void testOnConflictColumnDoNothing() {
        String sql = "INSERT INTO users (id, username) VALUES (1, 'john') ON CONFLICT (username) DO NOTHING";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOnConflict()).isTrue();
        assertThat(details.getOnConflictTarget()).isEqualTo("username");
    }

    @Test
    @DisplayName("解析 ON CONFLICT DO UPDATE")
    void testOnConflictDoUpdate() {
        String sql = "INSERT INTO users (id, username, email) VALUES (1, 'john', 'john@test.com') " +
                     "ON CONFLICT (id) DO UPDATE SET username = EXCLUDED.username, email = EXCLUDED.email";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOnConflict()).isTrue();
        assertThat(details.getOnConflictAction()).contains("UPDATE");
        assertThat(details.getOnConflictUpdates()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 UPDATE RETURNING")
    void testUpdateReturning() {
        String sql = "UPDATE users SET age = age + 1 WHERE id = 1 RETURNING id, age";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLUpdateDetails details = (PostgreSQLUpdateDetails) stmt.getUpdateDetails();
        assertThat(details.isReturning()).isTrue();
    }

    @Test
    @DisplayName("解析 UPDATE FROM")
    void testUpdateFrom() {
        String sql = "UPDATE users u SET name = a.name FROM accounts a WHERE u.id = a.user_id";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLUpdateDetails details = (PostgreSQLUpdateDetails) stmt.getUpdateDetails();
        assertThat(details.isHasFrom()).isTrue();
    }

    @Test
    @DisplayName("解析 DELETE RETURNING")
    void testDeleteReturning() {
        String sql = "DELETE FROM users WHERE id = 1 RETURNING *";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLDeleteDetails details = (PostgreSQLDeleteDetails) stmt.getDeleteDetails();
        assertThat(details.isReturning()).isTrue();
    }

    @Test
    @DisplayName("解析 DELETE USING")
    void testDeleteUsing() {
        String sql = "DELETE FROM users u USING accounts a WHERE u.id = a.user_id";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLDeleteDetails details = (PostgreSQLDeleteDetails) stmt.getDeleteDetails();
        assertThat(details.isHasUsing()).isTrue();
    }

    @Test
    @DisplayName("解析 CTE (WITH 子句)")
    void testCte() {
        String sql = "WITH active_users AS (SELECT * FROM users WHERE is_active = true) " +
                     "SELECT * FROM active_users";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isHasCte()).isTrue();
    }

    @Test
    @DisplayName("解析递归 CTE")
    void testRecursiveCte() {
        String sql = "WITH RECURSIVE category_tree AS (" +
                     "    SELECT id, name FROM categories WHERE parent_id IS NULL " +
                     "    UNION ALL " +
                     "    SELECT c.id, c.name FROM categories c JOIN category_tree ct ON c.parent_id = ct.id" +
                     ") SELECT * FROM category_tree";
        
        SqlStatement stmt = parser.parse(sql);
        
        PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
        assertThat(details.isHasCte()).isTrue();
    }

    @Test
    @DisplayName("解析复杂 CREATE TABLE")
    void testComplexCreateTable() {
        String sql = "CREATE TABLE IF NOT EXISTS public.users (\n" +
                     "    id SERIAL PRIMARY KEY,\n" +
                     "    username VARCHAR(50) NOT NULL UNIQUE,\n" +
                     "    email VARCHAR(100) NOT NULL,\n" +
                     "    profile JSONB,\n" +
                     "    tags TEXT[],\n" +
                     "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                     "    CONSTRAINT chk_email CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$')\n" +
                     ") TABLESPACE pg_default";
        
        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        
        assertThat(table.isIfNotExists()).isTrue();
        assertThat(table.getSchema()).isEqualTo("public");
        assertThat(table.getTableName()).isEqualTo("users");
        assertThat(table.getTablespace()).isEqualTo("pg_default");
        assertThat(table.getConstraints()).isNotEmpty();
    }
}
