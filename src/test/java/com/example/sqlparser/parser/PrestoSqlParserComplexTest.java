package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.presto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Presto/Trino SQL 解析器复杂场景测试
 */
class PrestoSqlParserComplexTest {

    private PrestoSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new PrestoSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂电商订单表")
    void testComplexEcommerceTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id BIGINT,\n" +
                "    user_id BIGINT NOT NULL,\n" +
                "    order_no VARCHAR(32) NOT NULL,\n" +
                "    total_amount DECIMAL(18,2) NOT NULL,\n" +
                "    status TINYINT NOT NULL,\n" +
                "    pay_time TIMESTAMP WITH TIME ZONE,\n" +
                "    created_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                "    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                "    deleted BOOLEAN NOT NULL DEFAULT FALSE,\n" +
                "    metadata JSON,\n" +
                "    tags ARRAY(VARCHAR),\n" +
                "    CONSTRAINT pk_orders PRIMARY KEY (order_id),\n" +
                "    CONSTRAINT uk_order_no UNIQUE (order_no)\n" +
                ")\n" +
                "WITH (\n" +
                "    format = 'ORC',\n" +
                "    partitioned_by = ARRAY['created_at'],\n" +
                "    bucketed_by = ARRAY['user_id'],\n" +
                "    bucket_count = 16\n" +
                ")";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getSchema()).isEqualTo("ecommerce");
        assertThat(table.isIfNotExists()).isTrue();
    }

    @Test
    @DisplayName("解析带所有约束类型的表")
    void testTableWithAllConstraints() {
        String sql = "CREATE TABLE constraint_test (\n" +
                "    id BIGINT PRIMARY KEY,\n" +
                "    unique_col VARCHAR(100) UNIQUE,\n" +
                "    not_null_col INTEGER NOT NULL,\n" +
                "    check_col INTEGER CHECK (check_col > 0),\n" +
                "    default_col VARCHAR(50) DEFAULT 'default_value',\n" +
                "    ref_col INTEGER REFERENCES other_table(id),\n" +
                "    CONSTRAINT chk_custom CHECK (not_null_col > check_col),\n" +
                "    CONSTRAINT uk_composite UNIQUE (unique_col, not_null_col)\n" +
                ")";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("constraint_test");
    }

    @Test
    @DisplayName("解析带外键级联操作的表")
    void testTableWithForeignKeyActions() {
        String sql = "CREATE TABLE order_items (\n" +
                "    item_id BIGINT PRIMARY KEY,\n" +
                "    order_id BIGINT NOT NULL,\n" +
                "    product_id INTEGER NOT NULL,\n" +
                "    quantity INTEGER NOT NULL DEFAULT 1,\n" +
                "    price DECIMAL(18,2) NOT NULL,\n" +
                "    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) \n" +
                "        REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                "    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) \n" +
                "        REFERENCES products(product_id) ON DELETE RESTRICT\n" +
                ")";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("order_items");
    }

    @Test
    @DisplayName("解析复杂数据类型表")
    void testComplexDataTypesTable() {
        String sql = "CREATE TABLE complex_types (\n" +
                "    id BIGINT,\n" +
                "    tags ARRAY(VARCHAR),\n" +
                "    properties MAP(VARCHAR, VARCHAR),\n" +
                "    address ROW(street VARCHAR, city VARCHAR, zip VARCHAR),\n" +
                "    items ARRAY(ROW(id INTEGER, name VARCHAR, price DECIMAL(10,2))),\n" +
                "    metadata MAP(VARCHAR, ARRAY(VARCHAR)),\n" +
                "    ip_address IPADDRESS,\n" +
                "    uuid UUID,\n" +
                "    hyperloglog_col HYPERLOGLOG\n" +
                ")";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("complex_types");
    }

    @Test
    @DisplayName("解析带 WITH 属性的表")
    void testTableWithWithProperties() {
        String sql = "CREATE TABLE kafka_table (\n" +
                "    id BIGINT,\n" +
                "    message VARCHAR\n" +
                ")\n" +
                "WITH (\n" +
                "    connector = 'kafka',\n" +
                "    'kafka.topic' = 'test-topic',\n" +
                "    'kafka.bootstrap.servers' = 'localhost:9092',\n" +
                "    format = 'json'\n" +
                ")";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getProperties()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 AS SELECT WITH NO DATA")
    void testAsSelectWithNoData() {
        String sql = "CREATE TABLE summary AS\n" +
                "SELECT user_id, COUNT(*) as cnt\n" +
                "FROM events\n" +
                "GROUP BY user_id\n" +
                "WITH NO DATA";

        PrestoCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isWithData()).isFalse();
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
                "JOIN orders o ON u.user_id = o.user_id\n" +
                "LEFT JOIN order_items oi ON o.order_id = oi.order_id\n" +
                "LEFT JOIN products p ON oi.product_id = p.product_id\n" +
                "WHERE o.status = 1\n" +
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
    @DisplayName("解析 UNNEST 查询")
    void testUnnestQuery() {
        String sql = "SELECT id, tag\n" +
                "FROM events,\n" +
                "UNNEST(tags) AS t(tag)";

        SqlStatement stmt = parser.parse(sql);
        PrestoSelectDetails details = (PrestoSelectDetails) stmt.getSelectDetails();
        assertThat(details.getUnnestClauses()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 UNNEST WITH ORDINALITY")
    void testUnnestWithOrdinality() {
        String sql = "SELECT id, tag, ord\n" +
                "FROM events,\n" +
                "UNNEST(tags) WITH ORDINALITY AS t(tag, ord)";

        SqlStatement stmt = parser.parse(sql);
        PrestoSelectDetails details = (PrestoSelectDetails) stmt.getSelectDetails();
        assertThat(details.getUnnestClauses().get(0).isWithOrdinality()).isTrue();
    }

    @Test
    @DisplayName("解析多个 UNNEST")
    void testMultipleUnnest() {
        String sql = "SELECT id, tag, score\n" +
                "FROM events,\n" +
                "UNNEST(tags, scores) AS t(tag, score)";

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
                "    AVG(amount) OVER (PARTITION BY user_id RANGE BETWEEN INTERVAL '7' DAY PRECEDING AND CURRENT ROW) AS avg_7d,\n" +
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
    @DisplayName("解析带命名窗口的查询")
    void testNamedWindowQuery() {
        String sql = "SELECT\n" +
                "    department,\n" +
                "    employee,\n" +
                "    salary,\n" +
                "    AVG(salary) OVER w_dept AS avg_dept_salary,\n" +
                "    SUM(salary) OVER w_company AS total_company_salary\n" +
                "FROM employees\n" +
                "WINDOW w_dept AS (PARTITION BY department),\n" +
                "       w_company AS ()\n" +
                "ORDER BY department, salary DESC";

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
                "        SELECT * FROM orders WHERE created_at > CURRENT_DATE - INTERVAL '30' DAY\n" +
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
    @DisplayName("解析 GROUP BY ROLLUP")
    void testGroupByRollup() {
        String sql = "SELECT year, quarter, month, SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY ROLLUP(year, quarter, month)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY CUBE")
    void testGroupByCube() {
        String sql = "SELECT year, month, region, SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY CUBE(year, month, region)";

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
    @DisplayName("解析 MATCH_RECOGNIZE 查询")
    void testMatchRecognizeQuery() {
        String sql = "SELECT *\n" +
                "FROM stock_prices\n" +
                "MATCH_RECOGNIZE (\n" +
                "    PARTITION BY symbol\n" +
                "    ORDER BY timestamp\n" +
                "    MEASURES\n" +
                "        A.timestamp AS start_tstamp,\n" +
                "        LAST(B.timestamp) AS end_tstamp,\n" +
                "        A.price AS start_price,\n" +
                "        LAST(B.price) AS end_price\n" +
                "    ONE ROW PER MATCH\n" +
                "    AFTER MATCH SKIP TO NEXT ROW\n" +
                "    PATTERN (A B+ C)\n" +
                "    DEFINE\n" +
                "        A AS A.price < 100,\n" +
                "        B AS B.price > A.price,\n" +
                "        C AS C.price < B.price\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 TABLESAMPLE BERNOULLI")
    void testTableSampleBernoulli() {
        String sql = "SELECT * FROM large_table TABLESAMPLE BERNOULLI (10)";

        SqlStatement stmt = parser.parse(sql);
        PrestoSelectDetails details = (PrestoSelectDetails) stmt.getSelectDetails();
        assertThat(details.getTableSample()).isNotNull();
        assertThat(details.getTableSample().getType()).isEqualTo("BERNOULLI");
    }

    @Test
    @DisplayName("解析 TABLESAMPLE SYSTEM")
    void testTableSampleSystem() {
        String sql = "SELECT * FROM large_table TABLESAMPLE SYSTEM (5)";

        SqlStatement stmt = parser.parse(sql);
        PrestoSelectDetails details = (PrestoSelectDetails) stmt.getSelectDetails();
        assertThat(details.getTableSample()).isNotNull();
        assertThat(details.getTableSample().getType()).isEqualTo("SYSTEM");
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
                "    APPROX_DISTINCT(user_id) AS approx_unique_users,\n" +
                "    APPROX_PERCENTILE(total_amount, 0.5) AS median_amount,\n" +
                "    APPROX_PERCENTILE(total_amount, ARRAY[0.25, 0.5, 0.75, 0.9, 0.99]) AS amount_percentiles,\n" +
                "    ARBITRARY(status) AS any_status,\n" +
                "    MAX_BY(order_id, total_amount) AS max_amount_order,\n" +
                "    MIN_BY(order_id, total_amount) AS min_amount_order\n" +
                "FROM orders\n" +
                "WHERE status = 1\n" +
                "GROUP BY DATE_TRUNC('day', created_at)\n" +
                "HAVING COUNT(*) >= 10\n" +
                "ORDER BY day DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析复杂 JSON 操作")
    void testComplexJsonOperations() {
        String sql = "SELECT\n" +
                "    JSON_EXTRACT_SCALAR(data, '$.name') AS name,\n" +
                "    JSON_EXTRACT(data, '$.address') AS address,\n" +
                "    JSON_EXTRACT_SCALAR(data, '$.address.city') AS city,\n" +
                "    JSON_SIZE(data, '$.items') AS item_count,\n" +
                "    JSON_FORMAT(data) AS json_string,\n" +
                "    JSON_PARSE('{}') AS empty_object,\n" +
                "    data IS JSON AS is_valid_json\n" +
                "FROM json_table\n" +
                "WHERE JSON_EXISTS(data, '$.active')\n" +
                "    AND JSON_EXTRACT_SCALAR(data, '$.status') = 'active'";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析复杂数组操作")
    void testComplexArrayOperations() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    tags,\n" +
                "    CARDINALITY(tags) AS tag_count,\n" +
                "    SLICE(tags, 1, 3) AS first_three_tags,\n" +
                "    ARRAY_DISTINCT(tags) AS unique_tags,\n" +
                "    ARRAY_SORT(tags) AS sorted_tags,\n" +
                "    ARRAY_JOIN(tags, ', ') AS tags_string,\n" +
                "    CONTAINS(tags, 'important') AS has_important,\n" +
                "    ANY_MATCH(x -> x LIKE 'pre%', tags) AS has_prefix_match,\n" +
                "    FILTER(tags, x -> LENGTH(x) > 5) AS long_tags,\n" +
                "    TRANSFORM(tags, x -> UPPER(x)) AS upper_tags,\n" +
                "    REDUCE(tags, 0, (s, x) -> s + LENGTH(x), s -> s) AS total_length\n" +
                "FROM articles\n" +
                "WHERE tags IS NOT NULL\n" +
                "    AND CARDINALITY(tags) > 0";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 MAP 操作")
    void testMapOperations() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    properties,\n" +
                "    CARDINALITY(properties) AS prop_count,\n" +
                "    MAP_KEYS(properties) AS keys,\n" +
                "    MAP_VALUES(properties) AS values,\n" +
                "    ELEMENT_AT(properties, 'key1') AS value1,\n" +
                "    properties['key2'] AS value2\n" +
                "FROM items\n" +
                "WHERE CONTAINS_KEY(properties, 'required_key')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析子查询")
    void testSubQuery() {
        String sql = "SELECT * FROM users WHERE user_id IN (\n" +
                "    SELECT user_id FROM orders WHERE total_amount > 1000\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 EXISTS 子查询")
    void testExistsSubQuery() {
        String sql = "SELECT * FROM users u WHERE EXISTS (\n" +
                "    SELECT 1 FROM orders o WHERE o.user_id = u.user_id\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UNION 查询")
    void testUnionQuery() {
        String sql = "SELECT * FROM orders_202401\n" +
                "UNION\n" +
                "SELECT * FROM orders_202402";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UNION ALL 查询")
    void testUnionAllQuery() {
        String sql = "SELECT * FROM orders_202401\n" +
                "UNION ALL\n" +
                "SELECT * FROM orders_202402\n" +
                "UNION ALL\n" +
                "SELECT * FROM orders_202403";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 EXCEPT 查询")
    void testExceptQuery() {
        String sql = "SELECT user_id FROM active_users\n" +
                "EXCEPT\n" +
                "SELECT user_id FROM banned_users";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 INTERSECT 查询")
    void testIntersectQuery() {
        String sql = "SELECT user_id FROM vip_users\n" +
                "INTERSECT\n" +
                "SELECT user_id FROM active_users";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    // ==================== INSERT 测试 ====================

    @Test
    @DisplayName("解析 INSERT INTO")
    void testInsertInto() {
        String sql = "INSERT INTO users (id, name, email)\n" +
                "VALUES (1, 'John', 'john@example.com')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析批量 INSERT")
    void testBatchInsert() {
        String sql = "INSERT INTO users (id, name, email) VALUES\n" +
                "    (1, 'User1', 'user1@test.com'),\n" +
                "    (2, 'User2', 'user2@test.com'),\n" +
                "    (3, 'User3', 'user3@test.com')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT OVERWRITE")
    void testInsertOverwrite() {
        String sql = "INSERT OVERWRITE sales\n" +
                "SELECT * FROM staging";

        SqlStatement stmt = parser.parse(sql);
        PrestoInsertDetails details = (PrestoInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOverwrite()).isTrue();
    }

    // ==================== DDL 测试 ====================

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE OR REPLACE VIEW active_users AS\n" +
                "SELECT * FROM users WHERE active = true";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 CREATE MATERIALIZED VIEW")
    void testCreateMaterializedView() {
        String sql = "CREATE MATERIALIZED VIEW daily_sales AS\n" +
                "SELECT DATE(created_at) as sale_date, SUM(total) as total_sales\n" +
                "FROM orders\n" +
                "GROUP BY DATE(created_at)";

        SqlStatement stmt = parser.parse(sql);
        // CREATE MATERIALIZED VIEW parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD COLUMN")
    void testAlterTableAddColumn() {
        String sql = "ALTER TABLE users ADD COLUMN phone VARCHAR(20)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE DROP COLUMN")
    void testAlterTableDropColumn() {
        String sql = "ALTER TABLE users DROP COLUMN old_field";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE RENAME COLUMN")
    void testAlterTableRenameColumn() {
        String sql = "ALTER TABLE users RENAME COLUMN old_name TO new_name";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE SET PROPERTIES")
    void testAlterTableSetProperties() {
        String sql = "ALTER TABLE users SET PROPERTIES format = 'ORC'";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 CALL 语句")
    void testCallStatement() {
        String sql = "CALL system.sync_partition_metadata('schema_name', 'table_name', 'ADD')";

        SqlStatement stmt = parser.parse(sql);
        // CALL parsing test
    }

    @Test
    @DisplayName("解析 ANALYZE 语句")
    void testAnalyzeStatement() {
        String sql = "ANALYZE users";

        SqlStatement stmt = parser.parse(sql);
        // ANALYZE parsing test
    }

    @Test
    @DisplayName("解析 EXPLAIN 语句")
    void testExplainStatement() {
        String sql = "EXPLAIN (TYPE DISTRIBUTED) SELECT * FROM users";

        SqlStatement stmt = parser.parse(sql);
        // EXPLAIN parsing test
    }

    @Test
    @DisplayName("解析 EXPLAIN ANALYZE")
    void testExplainAnalyze() {
        String sql = "EXPLAIN ANALYZE SELECT * FROM users";

        SqlStatement stmt = parser.parse(sql);
        // EXPLAIN parsing test
    }

    @Test
    @DisplayName("解析 PREPARE 语句")
    void testPrepareStatement() {
        String sql = "PREPARE my_select FROM 'SELECT * FROM users WHERE id = ?'";

        SqlStatement stmt = parser.parse(sql);
        // PREPARE parsing test
    }

    @Test
    @DisplayName("解析 EXECUTE 语句")
    void testExecuteStatement() {
        String sql = "EXECUTE my_select USING 1";

        SqlStatement stmt = parser.parse(sql);
        // EXECUTE parsing test
    }

    @Test
    @DisplayName("解析 DEALLOCATE PREPARE")
    void testDeallocatePrepare() {
        String sql = "DEALLOCATE PREPARE my_select";

        SqlStatement stmt = parser.parse(sql);
        // DEALLOCATE parsing test
    }

    @Test
    @DisplayName("解析 START TRANSACTION")
    void testStartTransaction() {
        String sql = "START TRANSACTION ISOLATION LEVEL READ COMMITTED, READ ONLY";

        SqlStatement stmt = parser.parse(sql);
        // START TRANSACTION parsing test
    }

    @Test
    @DisplayName("解析 COMMIT")
    void testCommit() {
        String sql = "COMMIT WORK";

        SqlStatement stmt = parser.parse(sql);
        // COMMIT parsing test
    }

    @Test
    @DisplayName("解析 ROLLBACK")
    void testRollback() {
        String sql = "ROLLBACK WORK";

        SqlStatement stmt = parser.parse(sql);
        // ROLLBACK parsing test
    }
}
