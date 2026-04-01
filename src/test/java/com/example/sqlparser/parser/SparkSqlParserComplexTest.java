package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.spark.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Spark SQL 解析器复杂场景测试
 */
class SparkSqlParserComplexTest {

    private SparkSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SparkSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂 Delta Lake 表")
    void testComplexDeltaTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id BIGINT,\n" +
                "    user_id BIGINT,\n" +
                "    order_no STRING,\n" +
                "    total_amount DECIMAL(18,2),\n" +
                "    status TINYINT,\n" +
                "    pay_time TIMESTAMP,\n" +
                "    created_at TIMESTAMP,\n" +
                "    updated_at TIMESTAMP,\n" +
                "    deleted BOOLEAN\n" +
                ")\n" +
                "USING DELTA\n" +
                "PARTITIONED BY (date STRING)\n" +
                "CLUSTERED BY (user_id) INTO 16 BUCKETS\n" +
                "LOCATION '/mnt/delta/orders'\n" +
                "TBLPROPERTIES (\n" +
                "    'delta.autoOptimize.optimizeWrite' = 'true',\n" +
                "    'delta.autoOptimize.autoCompact' = 'true',\n" +
                "    'delta.logRetentionDuration' = 'interval 30 days',\n" +
                "    'delta.deletedFileRetentionDuration' = 'interval 7 days'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getUsing()).isEqualTo("DELTA");
        assertThat(table.getPartitionedBy()).containsExactly("date STRING");
    }

    @Test
    @DisplayName("解析 Iceberg 表")
    void testIcebergTable() {
        String sql = "CREATE TABLE iceberg_table (\n" +
                "    id BIGINT,\n" +
                "    data STRING,\n" +
                "    category STRING\n" +
                ")\n" +
                "USING ICEBERG\n" +
                "PARTITIONED BY (days(created_at), category)\n" +
                "TBLPROPERTIES (\n" +
                "    'write_compression' = 'ZSTD',\n" +
                "    'commit.manifest.min-count-to-merge' = '5'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("ICEBERG");
    }

    @Test
    @DisplayName("解析 Hudi 表")
    void testHudiTable() {
        String sql = "CREATE TABLE hudi_table (\n" +
                "    id BIGINT,\n" +
                "    name STRING,\n" +
                "    ts TIMESTAMP\n" +
                ")\n" +
                "USING HUDI\n" +
                "PARTITIONED BY (dt STRING)\n" +
                "TBLPROPERTIES (\n" +
                "    'type' = 'cow',\n" +
                "    'primaryKey' = 'id',\n" +
                "    'preCombineField' = 'ts'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("HUDI");
    }

    @Test
    @DisplayName("解析 Parquet 表")
    void testParquetTable() {
        String sql = "CREATE TABLE parquet_table (\n" +
                "    id INT,\n" +
                "    name STRING,\n" +
                "    data BINARY\n" +
                ")\n" +
                "USING PARQUET\n" +
                "OPTIONS (\n" +
                "    'compression' = 'snappy',\n" +
                "    'parquet.block.size' = '134217728'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("PARQUET");
    }

    @Test
    @DisplayName("解析 ORC 表")
    void testOrcTable() {
        String sql = "CREATE TABLE orc_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "USING ORC\n" +
                "OPTIONS (\n" +
                "    'orc.compress' = 'ZLIB',\n" +
                "    'orc.stripe.size' = '268435456'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("ORC");
    }

    @Test
    @DisplayName("解析 JSON 表")
    void testJsonTable() {
        String sql = "CREATE TABLE json_table (\n" +
                "    id INT,\n" +
                "    data STRING\n" +
                ")\n" +
                "USING JSON\n" +
                "OPTIONS (\n" +
                "    'multiLine' = 'true',\n" +
                "    'primitivesAsString' = 'true'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("JSON");
    }

    @Test
    @DisplayName("解析 CSV 表")
    void testCsvTable() {
        String sql = "CREATE TABLE csv_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "USING CSV\n" +
                "OPTIONS (\n" +
                "    'header' = 'true',\n" +
                "    'delimiter' = ',',\n" +
                "    'quote' = '\"',\n" +
                "    'escape' = '\\',\n" +
                "    'inferSchema' = 'false',\n" +
                "    'nullValue' = 'NULL',\n" +
                "    'nanValue' = 'NaN',\n" +
                "    'dateFormat' = 'yyyy-MM-dd',\n" +
                "    'timestampFormat' = 'yyyy-MM-dd HH:mm:ss'\n" +
                ")\n" +
                "LOCATION '/path/to/csv'";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("CSV");
    }

    @Test
    @DisplayName("解析 JDBC 表")
    void testJdbcTable() {
        String sql = "CREATE TABLE jdbc_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "USING JDBC\n" +
                "OPTIONS (\n" +
                "    'url' = 'jdbc:postgresql://host:5432/database',\n" +
                "    'dbtable' = 'public.table',\n" +
                "    'user' = 'username',\n" +
                "    'password' = 'password',\n" +
                "    'driver' = 'org.postgresql.Driver'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("JDBC");
    }

    @Test
    @DisplayName("解析 Kafka 流表")
    void testKafkaTable() {
        String sql = "CREATE TABLE kafka_stream (\n" +
                "    key STRING,\n" +
                "    value STRING,\n" +
                "    topic STRING,\n" +
                "    partition INT,\n" +
                "    offset BIGINT,\n" +
                "    timestamp TIMESTAMP,\n" +
                "    timestampType INT\n" +
                ")\n" +
                "USING KAFKA\n" +
                "OPTIONS (\n" +
                "    'kafka.bootstrap.servers' = 'host1:9092,host2:9092',\n" +
                "    'subscribe' = 'topic1,topic2',\n" +
                "    'startingOffsets' = 'earliest',\n" +
                "    'failOnDataLoss' = 'false'\n" +
                ")";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getUsing()).isEqualTo("KAFKA");
    }

    @Test
    @DisplayName("解析复杂数据类型表")
    void testComplexDataTypesTable() {
        String sql = "CREATE TABLE complex_types (\n" +
                "    id INT,\n" +
                "    tags ARRAY<STRING>,\n" +
                "    properties MAP<STRING, STRING>,\n" +
                "    address STRUCT<street:STRING, city:STRING, zip:STRING>,\n" +
                "    items ARRAY<STRUCT<id:INT, name:STRING, price:DECIMAL(10,2)>>,\n" +
                "    metadata MAP<STRING, ARRAY<STRING>>,\n" +
                "    binary_data BINARY\n" +
                ")\n" +
                "USING PARQUET";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("complex_types");
    }

    @Test
    @DisplayName("解析全局临时表")
    void testGlobalTemporaryTable() {
        String sql = "CREATE GLOBAL TEMPORARY VIEW global_temp_view AS\n" +
                "SELECT * FROM users WHERE active = true";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 OR REPLACE 表")
    void testOrReplaceTable() {
        String sql = "CREATE OR REPLACE TABLE replace_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ") USING PARQUET";

        SparkCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isOrReplace()).isTrue();
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
    @DisplayName("解析 BROADCAST JOIN Hint")
    void testBroadcastHint() {
        String sql = "SELECT /*+ BROADCAST(small_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id = s.id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 MERGE JOIN Hint")
    void testMergeHint() {
        String sql = "SELECT /*+ MERGE(large_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id = s.id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 SHUFFLE HASH JOIN Hint")
    void testShuffleHashHint() {
        String sql = "SELECT /*+ SHUFFLE_HASH(large_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id = s.id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 SHUFFLE REPLICATE NL JOIN Hint")
    void testShuffleReplicateNLHint() {
        String sql = "SELECT /*+ SHUFFLE_REPLICATE_NL(small_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id > s.min_id AND l.id < s.max_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 REPARTITION Hint")
    void testRepartitionHint() {
        String sql = "SELECT /*+ REPARTITION(100) */ * FROM large_table";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 COALESCE Hint")
    void testCoalesceHint() {
        String sql = "SELECT /*+ COALESCE(10) */ * FROM large_table";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 LATERAL VIEW 查询")
    void testLateralViewQuery() {
        String sql = "SELECT\n" +
                "    e.id,\n" +
                "    tag\n" +
                "FROM events e\n" +
                "LATERAL VIEW explode(e.tags) t AS tag\n" +
                "WHERE tag IS NOT NULL";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLateralViews()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 LATERAL VIEW OUTER")
    void testLateralViewOuter() {
        String sql = "SELECT\n" +
                "    e.id,\n" +
                "    tag\n" +
                "FROM events e\n" +
                "LATERAL VIEW OUTER explode(e.tags) t AS tag";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLateralViews().get(0).isOuter()).isTrue();
    }

    @Test
    @DisplayName("解析 POSEXPLODE")
    void testPosexplode() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    pos,\n" +
                "    val\n" +
                "FROM events\n" +
                "LATERAL VIEW posexplode(array_col) t AS pos, val";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 INLINE")
    void testInline() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    street,\n" +
                "    city\n" +
                "FROM users\n" +
                "LATERAL VIEW inline(addresses) t AS street, city, zip";

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
                "    SUM(amount) OVER (PARTITION BY user_id ORDER BY order_date ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total,\n" +
                "    AVG(amount) OVER (PARTITION BY user_id) AS avg_amount,\n" +
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
    @DisplayName("解析 CTE 查询")
    void testCTEQuery() {
        String sql = "WITH\n" +
                "    active_users AS (\n" +
                "        SELECT user_id FROM users WHERE status = 'active'\n" +
                "    ),\n" +
                "    recent_orders AS (\n" +
                "        SELECT * FROM orders WHERE created_at >= '2024-01-01'\n" +
                "    )\n" +
                "SELECT au.user_id, COUNT(*) as order_count\n" +
                "FROM active_users au\n" +
                "JOIN recent_orders ro ON au.user_id = ro.user_id\n" +
                "GROUP BY au.user_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析复杂聚合查询")
    void testComplexAggregation() {
        String sql = "SELECT\n" +
                "    DATE(created_at) AS day,\n" +
                "    COUNT(*) AS total_orders,\n" +
                "    COUNT(DISTINCT user_id) AS unique_users,\n" +
                "    SUM(total_amount) AS total_revenue,\n" +
                "    AVG(total_amount) AS avg_order_value,\n" +
                "    MAX(total_amount) AS max_order,\n" +
                "    MIN(total_amount) AS min_order,\n" +
                "    PERCENTILE_APPROX(total_amount, 0.5) AS median_amount,\n" +
                "    COLLECT_SET(status) AS all_statuses,\n" +
                "    COLLECT_LIST(order_id) AS order_ids\n" +
                "FROM orders\n" +
                "WHERE status = 1\n" +
                "GROUP BY DATE(created_at)\n" +
                "HAVING COUNT(*) >= 10\n" +
                "ORDER BY day DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY ROLLUP")
    void testGroupByWithRollup() {
        String sql = "SELECT year, month, region, SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH ROLLUP";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY CUBE")
    void testGroupByWithCube() {
        String sql = "SELECT year, month, region, SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH CUBE";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY GROUPING SETS")
    void testGroupingSets() {
        String sql = "SELECT\n" +
                "    year,\n" +
                "    month,\n" +
                "    region,\n" +
                "    SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY GROUPING SETS (\n" +
                "    (year, month, region),\n" +
                "    (year, month),\n" +
                "    (year),\n" +
                "    ()\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 PIVOT 查询")
    void testPivotQuery() {
        String sql = "SELECT * FROM (\n" +
                "    SELECT year, quarter, amount FROM sales\n" +
                ")\n" +
                "PIVOT (\n" +
                "    SUM(amount) FOR quarter IN ('Q1', 'Q2', 'Q3', 'Q4')\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UNPIVOT 查询")
    void testUnpivotQuery() {
        String sql = "SELECT * FROM sales_data\n" +
                "UNPIVOT (\n" +
                "    amount FOR quarter IN (q1, q2, q3, q4)\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 DISTRIBUTE BY 查询")
    void testDistributeByQuery() {
        String sql = "SELECT * FROM users DISTRIBUTE BY user_id";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getDistributeBy()).containsExactly("user_id");
    }

    @Test
    @DisplayName("解析 CLUSTER BY 查询")
    void testClusterByQuery() {
        String sql = "SELECT * FROM users CLUSTER BY age";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getClusterBy()).containsExactly("age");
    }

    @Test
    @DisplayName("解析 SORT BY 查询")
    void testSortByQuery() {
        String sql = "SELECT * FROM users SORT BY name ASC, age DESC";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getSortBy()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 TABLESAMPLE 查询")
    void testTableSampleQuery() {
        String sql = "SELECT * FROM large_table TABLESAMPLE (10 PERCENT)";

        SqlStatement stmt = parser.parse(sql);
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        assertThat(details.getSample()).isNotNull();
    }

    @Test
    @DisplayName("解析 TABLESAMPLE BUCKET")
    void testTableSampleBucket() {
        String sql = "SELECT * FROM large_table TABLESAMPLE (BUCKET 3 OUT OF 32 ON id)";

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
    @DisplayName("解析 INSERT OVERWRITE")
    void testInsertOverwrite() {
        String sql = "INSERT OVERWRITE TABLE sales PARTITION(date='2024-01-01')\n" +
                "SELECT * FROM staging";

        SqlStatement stmt = parser.parse(sql);
        SparkInsertDetails details = (SparkInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOverwrite()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT INTO")
    void testInsertInto() {
        String sql = "INSERT INTO users\n" +
                "SELECT * FROM new_users";

        SqlStatement stmt = parser.parse(sql);
        SparkInsertDetails details = (SparkInsertDetails) stmt.getInsertDetails();
        assertThat(details.isInto()).isTrue();
    }

    @Test
    @DisplayName("解析动态分区插入")
    void testDynamicPartitionInsert() {
        String sql = "INSERT OVERWRITE TABLE logs PARTITION(date)\n" +
                "SELECT id, message, date FROM staging";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT OVERWRITE DIRECTORY")
    void testInsertOverwriteDirectory() {
        String sql = "INSERT OVERWRITE DIRECTORY '/path/to/output'\n" +
                "USING PARQUET\n" +
                "SELECT * FROM users";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    // ==================== DDL 测试 ====================

    @Test
    @DisplayName("解析 ALTER TABLE ADD COLUMNS")
    void testAlterTableAddColumns() {
        String sql = "ALTER TABLE users ADD COLUMNS (phone STRING, address STRING)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE CHANGE COLUMN")
    void testAlterTableChangeColumn() {
        String sql = "ALTER TABLE users CHANGE COLUMN old_name new_name STRING COMMENT 'New column'";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE SET TBLPROPERTIES")
    void testAlterTableSetTblProperties() {
        String sql = "ALTER TABLE users SET TBLPROPERTIES ('comment' = 'User table')";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE OR REPLACE VIEW active_users AS\n" +
                "SELECT * FROM users WHERE active = true";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 CACHE TABLE")
    void testCacheTable() {
        String sql = "CACHE TABLE users OPTIONS ('storageLevel' = 'MEMORY_AND_DISK')";

        SqlStatement stmt = parser.parse(sql);
        // CACHE parsing test
    }

    @Test
    @DisplayName("解析 UNCACHE TABLE")
    void testUncacheTable() {
        String sql = "UNCACHE TABLE IF EXISTS users";

        SqlStatement stmt = parser.parse(sql);
        // UNCACHE parsing test
    }

    @Test
    @DisplayName("解析 REFRESH TABLE")
    void testRefreshTable() {
        String sql = "REFRESH TABLE users";

        SqlStatement stmt = parser.parse(sql);
        // REFRESH parsing test
    }

    @Test
    @DisplayName("解析 ANALYZE TABLE")
    void testAnalyzeTable() {
        String sql = "ANALYZE TABLE users COMPUTE STATISTICS FOR ALL COLUMNS";

        SqlStatement stmt = parser.parse(sql);
        // ANALYZE parsing test
    }

    @Test
    @DisplayName("解析 REPAIR TABLE")
    void testRepairTable() {
        String sql = "MSCK REPAIR TABLE sales";

        SqlStatement stmt = parser.parse(sql);
        // MSCK parsing test
    }

    @Test
    @DisplayName("解析 OPTIMIZE (Delta Lake)")
    void testOptimize() {
        String sql = "OPTIMIZE delta_table ZORDER BY (user_id)";

        SqlStatement stmt = parser.parse(sql);
        // OPTIMIZE parsing test
    }

    @Test
    @DisplayName("解析 VACUUM (Delta Lake)")
    void testVacuum() {
        String sql = "VACUUM delta_table RETAIN 168 HOURS";

        SqlStatement stmt = parser.parse(sql);
        // VACUUM parsing test
    }

    @Test
    @DisplayName("解析 DESCRIBE HISTORY (Delta Lake)")
    void testDescribeHistory() {
        String sql = "DESCRIBE HISTORY delta_table";

        SqlStatement stmt = parser.parse(sql);
        // DESCRIBE_HISTORY parsing test
    }

    @Test
    @DisplayName("解析 CONVERT TO DELTA")
    void testConvertToDelta() {
        String sql = "CONVERT TO DELTA parquet.`/path/to/table`";

        SqlStatement stmt = parser.parse(sql);
        // CONVERT_TO_DELTA parsing test
    }
}
