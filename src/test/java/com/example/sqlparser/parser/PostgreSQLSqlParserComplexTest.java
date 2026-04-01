package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.postgresql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PostgreSQL SQL 解析器复杂场景测试
 */
class PostgreSQLSqlParserComplexTest {

    private PostgreSQLSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new PostgreSQLSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂电商订单表")
    void testComplexEcommerceTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id BIGSERIAL PRIMARY KEY,\n" +
                "    user_id BIGINT NOT NULL REFERENCES users(user_id),\n" +
                "    order_no VARCHAR(32) NOT NULL UNIQUE,\n" +
                "    total_amount NUMERIC(18,2) NOT NULL DEFAULT 0.00,\n" +
                "    status SMALLINT NOT NULL DEFAULT 0 CHECK (status >= 0 AND status <= 4),\n" +
                "    pay_time TIMESTAMP WITH TIME ZONE,\n" +
                "    ship_time TIMESTAMP WITH TIME ZONE,\n" +
                "    receive_time TIMESTAMP WITH TIME ZONE,\n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                "    deleted BOOLEAN NOT NULL DEFAULT FALSE,\n" +
                "    metadata JSONB DEFAULT '{}',\n" +
                "    tags TEXT[] DEFAULT ARRAY[]::TEXT[],\n" +
                "    CONSTRAINT chk_total_amount CHECK (total_amount >= 0),\n" +
                "    CONSTRAINT chk_order_no_format CHECK (order_no ~ '^[A-Z0-9]{10,32}$')\n" +
                ") PARTITION BY RANGE (created_at);";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getSchema()).isEqualTo("ecommerce");
        assertThat(table.isIfNotExists()).isTrue();
        assertThat(table.getPartitionInfo()).isNotNull();
        assertThat(table.getPartitionInfo().getPartitionType()).isEqualTo("RANGE");
    }

    @Test
    @DisplayName("解析带所有约束类型的表")
    void testTableWithAllConstraints() {
        String sql = "CREATE TABLE constraint_test (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    unique_col VARCHAR(100) UNIQUE,\n" +
                "    not_null_col INTEGER NOT NULL,\n" +
                "    check_col INTEGER CHECK (check_col > 0),\n" +
                "    default_col VARCHAR(50) DEFAULT 'default_value',\n" +
                "    ref_col INTEGER REFERENCES other_table(id),\n" +
                "    CONSTRAINT chk_custom CHECK (not_null_col > check_col),\n" +
                "    CONSTRAINT uk_composite UNIQUE (unique_col, not_null_col)\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("constraint_test");
        assertThat(table.getConstraints()).isNotEmpty();
    }

    @Test
    @DisplayName("解析带外键级联操作的表")
    void testTableWithForeignKeyActions() {
        String sql = "CREATE TABLE order_items (\n" +
                "    item_id SERIAL PRIMARY KEY,\n" +
                "    order_id BIGINT NOT NULL,\n" +
                "    product_id INTEGER NOT NULL,\n" +
                "    quantity INTEGER NOT NULL DEFAULT 1,\n" +
                "    price NUMERIC(18,2) NOT NULL,\n" +
                "    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) \n" +
                "        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                "    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) \n" +
                "        REFERENCES products(product_id) ON DELETE RESTRICT ON UPDATE NO ACTION,\n" +
                "    CONSTRAINT fk_order_items_category FOREIGN KEY (category_id) \n" +
                "        REFERENCES categories(id) ON DELETE SET NULL ON UPDATE SET DEFAULT\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("order_items");
    }

    @Test
    @DisplayName("解析带生成列的表")
    void testTableWithGeneratedColumns() {
        String sql = "CREATE TABLE generated_cols (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    full_name VARCHAR(101) GENERATED ALWAYS AS (first_name || ' ' || last_name) STORED,\n" +
                "    birth_date DATE,\n" +
                "    age INTEGER GENERATED ALWAYS AS (EXTRACT(YEAR FROM AGE(birth_date))) STORED,\n" +
                "    total NUMERIC(10,2),\n" +
                "    tax NUMERIC(10,2) GENERATED ALWAYS AS (total * 0.13) STORED\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("generated_cols");
    }

    @Test
    @DisplayName("解析带自定义类型的表")
    void testTableWithCustomTypes() {
        String sql = "CREATE TABLE custom_type_test (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    coordinates POINT,\n" +
                "    bounds BOX,\n" +
                "    path PATH,\n" +
                "    polygon POLYGON,\n" +
                "    circle CIRCLE,\n" +
                "    ip_address INET,\n" +
                "    mac_address MACADDR,\n" +
                "    uuid UUID,\n" +
                "    tsvector_col TSVECTOR,\n" +
                "    tsquery_col TSQUERY\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("custom_type_test");
    }

    @Test
    @DisplayName("解析 RANGE 分区表带具体分区")
    void testRangePartitionWithPartitions() {
        String sql = "CREATE TABLE events (\n" +
                "    event_id BIGSERIAL,\n" +
                "    event_time TIMESTAMP NOT NULL,\n" +
                "    user_id BIGINT,\n" +
                "    event_type VARCHAR(50)\n" +
                ") PARTITION BY RANGE (event_time) (\n" +
                "    PARTITION events_2023_q1 VALUES LESS THAN ('2023-04-01'),\n" +
                "    PARTITION events_2023_q2 VALUES LESS THAN ('2023-07-01'),\n" +
                "    PARTITION events_2023_q3 VALUES LESS THAN ('2023-10-01'),\n" +
                "    PARTITION events_2023_q4 VALUES LESS THAN ('2024-01-01'),\n" +
                "    PARTITION events_future VALUES LESS THAN (MAXVALUE)\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("events");
        assertThat(table.getPartitionInfo()).isNotNull();
    }

    @Test
    @DisplayName("解析 LIST 分区表")
    void testListPartitionTable() {
        String sql = "CREATE TABLE sales_by_region (\n" +
                "    sale_id SERIAL,\n" +
                "    region VARCHAR(20) NOT NULL,\n" +
                "    amount NUMERIC(18,2)\n" +
                ") PARTITION BY LIST (region) (\n" +
                "    PARTITION p_north VALUES ('North'),\n" +
                "    PARTITION p_south VALUES ('South'),\n" +
                "    PARTITION p_east VALUES ('East'),\n" +
                "    PARTITION p_west VALUES ('West')\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_by_region");
    }

    @Test
    @DisplayName("解析带继承的表")
    void testTableWithInheritance() {
        String sql = "CREATE TABLE employees (\n" +
                "    employee_id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR(100) NOT NULL,\n" +
                "    department VARCHAR(50)\n" +
                ") INHERITS (persons)";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("employees");
        assertThat(table.getInherits()).contains("persons");
    }

    @Test
    @DisplayName("解析带表空间的表")
    void testTableWithTablespace() {
        String sql = "CREATE TABLE large_table (\n" +
                "    id BIGSERIAL PRIMARY KEY,\n" +
                "    data BYTEA\n" +
                ") TABLESPACE fast_ssd";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("large_table");
        assertThat(table.getTablespace()).isEqualTo("fast_ssd");
    }

    @Test
    @DisplayName("解析带存储参数的表")
    void testTableWithStorageParameters() {
        String sql = "CREATE TABLE storage_test (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    data TEXT\n" +
                ") WITH (\n" +
                "    fillfactor = 70,\n" +
                "    autovacuum_enabled = true,\n" +
                "    autovacuum_vacuum_scale_factor = 0.1\n" +
                ")";

        PostgreSQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("storage_test");
    }

    // ==================== 复杂 SELECT 测试 ====================

    @Test
    @DisplayName("解析复杂 JOIN 查询")
    void testComplexJoinQuery() {
        String sql = "SELECT \n" +
                "    u.user_id,\n" +
                "    u.username,\n" +
                "    o.order_id,\n" +
                "    o.total_amount,\n" +
                "    p.product_name,\n" +
                "    oi.quantity\n" +
                "FROM users u\n" +
                "INNER JOIN orders o ON u.user_id = o.user_id\n" +
                "LEFT JOIN order_items oi ON o.order_id = oi.order_id\n" +
                "LEFT JOIN products p ON oi.product_id = p.product_id\n" +
                "LEFT JOIN LATERAL (\n" +
                "    SELECT * FROM inventory WHERE product_id = p.product_id LIMIT 1\n" +
                ") inv ON true\n" +
                "WHERE o.status = 1\n" +
                "    AND o.created_at >= '2024-01-01'::date\n" +
                "ORDER BY o.created_at DESC\n" +
                "LIMIT 100";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 LATERAL JOIN 查询")
    void testLateralJoinQuery() {
        String sql = "SELECT u.name, recent_orders.*\n" +
                "FROM users u,\n" +
                "LATERAL (\n" +
                "    SELECT * FROM orders\n" +
                "    WHERE user_id = u.user_id\n" +
                "    ORDER BY created_at DESC\n" +
                "    LIMIT 5\n" +
                ") recent_orders";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析递归 CTE 查询")
    void testRecursiveCTEQuery() {
        String sql = "WITH RECURSIVE category_tree AS (\n" +
                "    SELECT id, name, parent_id, 0 AS level, ARRAY[id] AS path\n" +
                "    FROM categories\n" +
                "    WHERE parent_id IS NULL\n" +
                "    UNION ALL\n" +
                "    SELECT c.id, c.name, c.parent_id, ct.level + 1, ct.path || c.id\n" +
                "    FROM categories c\n" +
                "    JOIN category_tree ct ON c.parent_id = ct.id\n" +
                "    WHERE NOT c.id = ANY(ct.path)\n" +
                ")\n" +
                "SELECT * FROM category_tree ORDER BY path";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析多个 CTE 查询")
    void testMultipleCTEs() {
        String sql = "WITH\n" +
                "    active_users AS (\n" +
                "        SELECT user_id FROM users WHERE status = 'active'\n" +
                "    ),\n" +
                "    recent_orders AS (\n" +
                "        SELECT * FROM orders WHERE created_at > NOW() - INTERVAL '30 days'\n" +
                "    ),\n" +
                "    user_stats AS (\n" +
                "        SELECT user_id, COUNT(*) as order_count, SUM(total) as total_spent\n" +
                "        FROM recent_orders\n" +
                "        GROUP BY user_id\n" +
                "    )\n" +
                "SELECT au.user_id, COALESCE(us.order_count, 0), COALESCE(us.total_spent, 0)\n" +
                "FROM active_users au\n" +
                "LEFT JOIN user_stats us ON au.user_id = us.user_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析窗口函数查询")
    void testWindowFunctionQuery() {
        String sql = "SELECT\n" +
                "    user_id,\n" +
                "    order_date,\n" +
                "    amount,\n" +
                "    SUM(amount) OVER (PARTITION BY user_id ORDER BY order_date ROWS UNBOUNDED PRECEDING) AS running_total,\n" +
                "    AVG(amount) OVER (PARTITION BY user_id RANGE BETWEEN INTERVAL '7 days' PRECEDING AND CURRENT ROW) AS avg_7d,\n" +
                "    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY order_date DESC) AS rn,\n" +
                "    RANK() OVER (ORDER BY amount DESC) AS rank_amount,\n" +
                "    DENSE_RANK() OVER (ORDER BY amount DESC) AS dense_rank_amount,\n" +
                "    PERCENT_RANK() OVER (ORDER BY amount) AS percent_rank,\n" +
                "    CUME_DIST() OVER (ORDER BY amount) AS cume_dist,\n" +
                "    NTILE(4) OVER (ORDER BY amount) AS quartile,\n" +
                "    LAG(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date) AS prev_amount,\n" +
                "    LEAD(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date) AS next_amount,\n" +
                "    FIRST_VALUE(amount) OVER (PARTITION BY user_id ORDER BY order_date) AS first_amount,\n" +
                "    LAST_VALUE(amount) OVER (PARTITION BY user_id ORDER BY order_date ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS last_amount,\n" +
                "    NTH_VALUE(amount, 2) OVER (PARTITION BY user_id ORDER BY order_date) AS second_amount\n" +
                "FROM orders";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY GROUPING SETS")
    void testGroupingSets() {
        String sql = "SELECT\n" +
                "    COALESCE(region, 'ALL') AS region,\n" +
                "    COALESCE(product, 'ALL') AS product,\n" +
                "    SUM(sales) AS total_sales\n" +
                "FROM sales_data\n" +
                "GROUP BY GROUPING SETS (\n" +
                "    (region, product),\n" +
                "    (region),\n" +
                "    (product),\n" +
                "    ()\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 FILTER 子句")
    void testFilterClause() {
        String sql = "SELECT\n" +
                "    department,\n" +
                "    COUNT(*) AS total_employees,\n" +
                "    COUNT(*) FILTER (WHERE active = true) AS active_employees,\n" +
                "    AVG(salary) FILTER (WHERE active = true) AS avg_active_salary\n" +
                "FROM employees\n" +
                "GROUP BY department";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析全文搜索查询")
    void testFullTextSearch() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    title,\n" +
                "    ts_rank(search_vector, query) AS rank\n" +
                "FROM articles,\n" +
                "    plainto_tsquery('english', 'PostgreSQL database') query\n" +
                "WHERE search_vector @@ query\n" +
                "ORDER BY rank DESC\n" +
                "LIMIT 20";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 JSON 操作查询")
    void testJsonOperations() {
        String sql = "SELECT\n" +
                "    data->>'name' AS name,\n" +
                "    data->>'email' AS email,\n" +
                "    data->'address'->>'city' AS city,\n" +
                "    data->'tags' AS tags,\n" +
                "    jsonb_array_length(data->'items') AS item_count,\n" +
                "    jsonb_each_text(data->'metadata') AS metadata_kv\n" +
                "FROM json_table\n" +
                "WHERE data @> '{\"active\": true}'\n" +
                "    AND data ? 'email'";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析数组操作查询")
    void testArrayOperations() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    tags,\n" +
                "    array_length(tags, 1) AS tag_count,\n" +
                "    unnest(tags) AS tag,\n" +
                "    tags && ARRAY['important', 'urgent'] AS has_priority_tags\n" +
                "FROM articles\n" +
                "WHERE tags @> ARRAY['database']\n" +
                "    AND 'postgresql' = ANY(tags)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UPSERT 查询")
    void testUpsertQuery() {
        String sql = "INSERT INTO users (id, name, email)\n" +
                "VALUES (1, 'John', 'john@example.com')\n" +
                "ON CONFLICT (id) DO UPDATE SET\n" +
                "    name = EXCLUDED.name,\n" +
                "    email = EXCLUDED.email,\n" +
                "    updated_at = CURRENT_TIMESTAMP\n" +
                "WHERE users.status = 'pending'\n" +
                "RETURNING id, name, email, updated_at";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析复杂聚合查询")
    void testComplexAggregation() {
        String sql = "SELECT\n" +
                "    DATE_TRUNC('day', created_at) AS day,\n" +
                "    COUNT(*) AS total_orders,\n" +
                "    COUNT(DISTINCT user_id) AS unique_users,\n" +
                "    SUM(total_amount) AS total_revenue,\n" +
                "    AVG(total_amount) AS avg_order_value,\n" +
                "    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY total_amount) AS median_amount,\n" +
                "    MODE() WITHIN GROUP (ORDER BY status) AS most_common_status,\n" +
                "    STRING_AGG(DISTINCT status::text, ', ' ORDER BY status::text) AS all_statuses\n" +
                "FROM orders\n" +
                "WHERE status = 1\n" +
                "GROUP BY DATE_TRUNC('day', created_at)\n" +
                "HAVING COUNT(*) >= 10\n" +
                "ORDER BY day DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    // ==================== 复杂 DML 测试 ====================

    @Test
    @DisplayName("解析批量 INSERT")
    void testBatchInsert() {
        String sql = "INSERT INTO users (username, email, age) VALUES\n" +
                "    ('user1', 'user1@test.com', 25),\n" +
                "    ('user2', 'user2@test.com', 30),\n" +
                "    ('user3', 'user3@test.com', 35),\n" +
                "    ('user4', 'user4@test.com', 28)\n" +
                "RETURNING id, username";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT ... ON CONFLICT DO NOTHING")
    void testInsertOnConflictDoNothing() {
        String sql = "INSERT INTO users (id, username) VALUES (1, 'john')\n" +
                "ON CONFLICT (username) DO NOTHING\n" +
                "RETURNING id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 UPDATE FROM")
    void testUpdateFrom() {
        String sql = "UPDATE orders o\n" +
                "SET status = 'shipped',\n" +
                "    ship_date = CURRENT_DATE\n" +
                "FROM customers c\n" +
                "WHERE o.customer_id = c.customer_id\n" +
                "    AND c.vip = true\n" +
                "    AND o.status = 'paid'\n" +
                "RETURNING o.order_id, o.status, c.name";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isUpdate()).isTrue();
    }

    @Test
    @DisplayName("解析 DELETE USING")
    void testDeleteUsing() {
        String sql = "DELETE FROM orders o\n" +
                "USING customers c\n" +
                "WHERE o.customer_id = c.customer_id\n" +
                "    AND c.status = 'inactive'\n" +
                "    AND o.created_at < NOW() - INTERVAL '1 year'\n" +
                "RETURNING o.order_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isDelete()).isTrue();
    }

    @Test
    @DisplayName("解析 MERGE 语句")
    void testMergeStatement() {
        String sql = "MERGE INTO target_table t\n" +
                "USING source_table s ON t.id = s.id\n" +
                "WHEN MATCHED THEN\n" +
                "    UPDATE SET name = s.name, updated_at = CURRENT_TIMESTAMP\n" +
                "    WHERE t.status != 'deleted'\n" +
                "WHEN NOT MATCHED THEN\n" +
                "    INSERT (id, name, created_at) VALUES (s.id, s.name, CURRENT_TIMESTAMP)";

        SqlStatement stmt = parser.parse(sql);
        // MERGE parsing test
    }

    // ==================== DDL 测试 ====================

    @Test
    @DisplayName("解析 CREATE INDEX")
    void testCreateIndex() {
        String sql = "CREATE UNIQUE INDEX CONCURRENTLY idx_users_email ON users USING btree (email)\n" +
                "WHERE deleted = false";

        SqlStatement stmt = parser.parse(sql);
        // CREATE INDEX parsing test
    }

    @Test
    @DisplayName("解析 CREATE INDEX WITH 参数")
    void testCreateIndexWithParameters() {
        String sql = "CREATE INDEX idx_gin_data ON documents USING gin (data jsonb_path_ops)\n" +
                "WITH (fastupdate = off)";

        SqlStatement stmt = parser.parse(sql);
        // CREATE INDEX parsing test
    }

    @Test
    @DisplayName("解析 CREATE TYPE")
    void testCreateType() {
        String sql = "CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy')";

        SqlStatement stmt = parser.parse(sql);
        // CREATE TYPE parsing test
    }

    @Test
    @DisplayName("解析 CREATE DOMAIN")
    void testCreateDomain() {
        String sql = "CREATE DOMAIN email AS VARCHAR(255)\n" +
                "CHECK (VALUE ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$')";

        SqlStatement stmt = parser.parse(sql);
        // CREATE DOMAIN parsing test
    }

    @Test
    @DisplayName("解析 CREATE FUNCTION")
    void testCreateFunction() {
        String sql = "CREATE OR REPLACE FUNCTION calculate_tax(amount NUMERIC)\n" +
                "RETURNS NUMERIC AS $$\n" +
                "BEGIN\n" +
                "    RETURN amount * 0.13;\n" +
                "END;\n" +
                "$$ LANGUAGE plpgsql IMMUTABLE";

        SqlStatement stmt = parser.parse(sql);
        // CREATE FUNCTION parsing test
    }

    @Test
    @DisplayName("解析 CREATE TRIGGER")
    void testCreateTrigger() {
        String sql = "CREATE TRIGGER update_timestamp\n" +
                "BEFORE UPDATE ON orders\n" +
                "FOR EACH ROW\n" +
                "EXECUTE FUNCTION update_updated_at_column()";

        SqlStatement stmt = parser.parse(sql);
        // CREATE TRIGGER parsing test
    }

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE OR REPLACE VIEW active_orders AS\n" +
                "SELECT * FROM orders WHERE status IN ('pending', 'processing')\n" +
                "WITH CHECK OPTION";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 CREATE MATERIALIZED VIEW")
    void testCreateMaterializedView() {
        String sql = "CREATE MATERIALIZED VIEW daily_sales AS\n" +
                "SELECT DATE(created_at) as sale_date, SUM(total) as total_sales\n" +
                "FROM orders\n" +
                "GROUP BY DATE(created_at)\n" +
                "WITH DATA";

        SqlStatement stmt = parser.parse(sql);
        // CREATE MATERIALIZED VIEW parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE 多种操作")
    void testAlterTableMultipleOperations() {
        String sql = "ALTER TABLE users\n" +
                "    ADD COLUMN phone VARCHAR(20),\n" +
                "    ADD COLUMN address TEXT,\n" +
                "    ALTER COLUMN email SET NOT NULL,\n" +
                "    DROP COLUMN old_field,\n" +
                "    ADD CONSTRAINT unique_email UNIQUE (email),\n" +
                "    DROP CONSTRAINT IF EXISTS old_constraint";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD COLUMN")
    void testAlterTableAddColumn() {
        String sql = "ALTER TABLE users ADD COLUMN phone VARCHAR(20)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }
}