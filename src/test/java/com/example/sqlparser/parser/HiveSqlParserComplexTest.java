package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.hive.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Hive SQL 解析器复杂场景测试
 */
class HiveSqlParserComplexTest {

    private HiveSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new HiveSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂分区表")
    void testComplexPartitionedTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id BIGINT,\n" +
                "    user_id BIGINT,\n" +
                "    order_no STRING,\n" +
                "    total_amount DECIMAL(18,2),\n" +
                "    status TINYINT,\n" +
                "    pay_time TIMESTAMP,\n" +
                "    created_at TIMESTAMP\n" +
                ")\n" +
                "PARTITIONED BY (dt STRING, hour STRING)\n" +
                "CLUSTERED BY (user_id) INTO 16 BUCKETS\n" +
                "SORTED BY (created_at DESC) INTO 16 BUCKETS\n" +
                "STORED AS ORC\n" +
                "TBLPROPERTIES (\n" +
                "    'orc.compress'='ZLIB',\n" +
                "    'orc.stripe.size'='268435456',\n" +
                "    'orc.row.index.stride'='10000',\n" +
                "    'transactional'='true',\n" +
                "    'compactor.mapreduce.map.memory.mb'='2048',\n" +
                "    'compactorthreshold.hive.compactor.delta.num.threshold'='10',\n" +
                "    'compactorthreshold.hive.compactor.delta.pct.threshold'='0.001'\n" +
                ")";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getPartitionColumns()).hasSize(2);
        assertThat(table.getStoredAs()).isEqualTo("ORC");
        assertThat(table.getProperties()).isNotEmpty();
    }

    @Test
    @DisplayName("解析复杂分桶表")
    void testComplexBucketedTable() {
        String sql = "CREATE TABLE user_events (\n" +
                "    user_id BIGINT,\n" +
                "    event_time TIMESTAMP,\n" +
                "    event_type STRING,\n" +
                "    event_data STRING\n" +
                ")\n" +
                "CLUSTERED BY (user_id) SORTED BY (event_time DESC) INTO 32 BUCKETS\n" +
                "STORED AS ORC\n" +
                "TBLPROPERTIES ('orc.compress'='SNAPPY')";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("user_events");
        assertThat(table.getClusteredBy()).containsExactly("user_id");
        assertThat(table.getNumBuckets()).isEqualTo(32);
    }

    @Test
    @DisplayName("解析 Skewed 表")
    void testSkewedTable() {
        String sql = "CREATE TABLE skewed_orders (\n" +
                "    order_id BIGINT,\n" +
                "    user_id BIGINT,\n" +
                "    status STRING\n" +
                ")\n" +
                "SKEWED BY (status) ON ('completed', 'cancelled')\n" +
                "STORED AS DIRECTORIES\n" +
                "STORED AS ORC";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("skewed_orders");
    }

    @Test
    @DisplayName("解析事务表")
    void testTransactionalTable() {
        String sql = "CREATE TABLE transactional_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "CLUSTERED BY (id) INTO 3 BUCKETS\n" +
                "STORED AS ORC\n" +
                "TBLPROPERTIES ('transactional'='true')";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("transactional_table");
    }

    @Test
    @DisplayName("解析外部表")
    void testExternalTable() {
        String sql = "CREATE EXTERNAL TABLE external_logs (\n" +
                "    log_time STRING,\n" +
                "    level STRING,\n" +
                "    message STRING\n" +
                ")\n" +
                "PARTITIONED BY (dt STRING)\n" +
                "ROW FORMAT DELIMITED\n" +
                "FIELDS TERMINATED BY '\t'\n" +
                "LINES TERMINATED BY '\n'\n" +
                "STORED AS TEXTFILE\n" +
                "LOCATION '/user/hive/external/logs'";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
        assertThat(table.getLocation()).isEqualTo("/user/hive/external/logs");
    }

    @Test
    @DisplayName("解析 JSON 格式表")
    void testJsonTable() {
        String sql = "CREATE TABLE json_events (\n" +
                "    user_id BIGINT,\n" +
                "    event STRING\n" +
                ")\n" +
                "ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'\n" +
                "STORED AS TEXTFILE";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("json_events");
    }

    @Test
    @DisplayName("解析 Parquet 格式表")
    void testParquetTable() {
        String sql = "CREATE TABLE parquet_table (\n" +
                "    id INT,\n" +
                "    name STRING,\n" +
                "    data BINARY\n" +
                ")\n" +
                "STORED AS PARQUET\n" +
                "TBLPROPERTIES (\n" +
                "    'parquet.compression'='GZIP',\n" +
                "    'parquet.block.size'='134217728'\n" +
                ")";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getStoredAs()).isEqualTo("PARQUET");
    }

    @Test
    @DisplayName("解析 Avro 格式表")
    void testAvroTable() {
        String sql = "CREATE TABLE avro_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe'\n" +
                "STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'\n" +
                "OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'\n" +
                "TBLPROPERTIES ('avro.schema.url'='hdfs:///path/to/schema.avsc')";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("avro_table");
    }

    @Test
    @DisplayName("解析 RCFile 格式表")
    void testRCFileTable() {
        String sql = "CREATE TABLE rcfile_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.columnar.ColumnarSerDe'\n" +
                "STORED AS RCFILE";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getStoredAs()).isEqualTo("RCFILE");
    }

    @Test
    @DisplayName("解析 SequenceFile 格式表")
    void testSequenceFileTable() {
        String sql = "CREATE TABLE sequence_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "ROW FORMAT DELIMITED\n" +
                "FIELDS TERMINATED BY ','\n" +
                "STORED AS SEQUENCEFILE";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getStoredAs()).isEqualTo("SEQUENCEFILE");
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
                "    metadata MAP<STRING, ARRAY<STRING>>\n" +
                ")\n" +
                "STORED AS ORC";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("complex_types");
    }

    @Test
    @DisplayName("解析带 SERDE 属性的表")
    void testTableWithSerdeProperties() {
        String sql = "CREATE TABLE serde_table (\n" +
                "    id INT,\n" +
                "    name STRING\n" +
                ")\n" +
                "ROW FORMAT SERDE 'com.example.CustomSerDe'\n" +
                "WITH SERDEPROPERTIES (\n" +
                "    'field.delim'=',',\n" +
                "    'serialization.format'='1'\n" +
                ")\n" +
                "STORED AS TEXTFILE";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("serde_table");
    }

    @Test
    @DisplayName("解析带文件格式的表")
    void testTableWithFileFormat() {
        String sql = "CREATE TABLE file_format_table (\n" +
                "    id INT,\n" +
                "    data STRING\n" +
                ")\n" +
                "STORED AS INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat'\n" +
                "OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'\n" +
                "LOCATION '/user/hive/data'";

        HiveCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("file_format_table");
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
                "    AND o.dt = '2024-01-01'\n" +
                "ORDER BY o.created_at DESC\n" +
                "LIMIT 100";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 MAP JOIN")
    void testMapJoin() {
        String sql = "SELECT /*+ MAPJOIN(small_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id = s.id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 SORT MERGE JOIN")
    void testSortMergeJoin() {
        String sql = "SELECT /*+ STREAMTABLE(large_table) */ *\n" +
                "FROM large_table l\n" +
                "JOIN small_table s ON l.id = s.id";

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
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLateralViews()).isNotEmpty();
    }

    @Test
    @DisplayName("解析多个 LATERAL VIEW")
    void testMultipleLateralViews() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    tag,\n" +
                "    score\n" +
                "FROM events\n" +
                "LATERAL VIEW explode(tags) t1 AS tag\n" +
                "LATERAL VIEW explode(scores) t2 AS score";

        SqlStatement stmt = parser.parse(sql);
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
        assertThat(details.getLateralViews()).hasSize(2);
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
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
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
                "        SELECT * FROM orders WHERE dt >= '2024-01-01'\n" +
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
                "    dt,\n" +
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
                "GROUP BY dt\n" +
                "HAVING COUNT(*) >= 10\n" +
                "ORDER BY dt DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH ROLLUP")
    void testGroupByWithRollup() {
        String sql = "SELECT year, month, region, SUM(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH ROLLUP";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH CUBE")
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
                "    COALESCE(region, 'ALL') AS region,\n" +
                "    COALESCE(product, 'ALL') AS product,\n" +
                "    SUM(sales) AS total_sales\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH ROLLUP";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 TRANSFORM 查询")
    void testTransformQuery() {
        String sql = "SELECT\n" +
                "    TRANSFORM(id, name, age)\n" +
                "    USING 'python /path/to/script.py'\n" +
                "    AS (new_id, new_name, new_age)\n" +
                "FROM users";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 DISTRIBUTE BY 查询")
    void testDistributeByQuery() {
        String sql = "SELECT * FROM users DISTRIBUTE BY user_id SORT BY user_id";

        SqlStatement stmt = parser.parse(sql);
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
        assertThat(details.getDistributeBy()).containsExactly("user_id");
    }

    @Test
    @DisplayName("解析 CLUSTER BY 查询")
    void testClusterByQuery() {
        String sql = "SELECT * FROM users CLUSTER BY age";

        SqlStatement stmt = parser.parse(sql);
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
        assertThat(details.getClusterBy()).containsExactly("age");
    }

    @Test
    @DisplayName("解析 SORT BY 查询")
    void testSortByQuery() {
        String sql = "SELECT * FROM users SORT BY name ASC, age DESC";

        SqlStatement stmt = parser.parse(sql);
        HiveSelectDetails details = (HiveSelectDetails) stmt.getSelectDetails();
        assertThat(details.getSortBy()).isNotEmpty();
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

    // ==================== INSERT 测试 ====================

    @Test
    @DisplayName("解析 INSERT OVERWRITE")
    void testInsertOverwrite() {
        String sql = "INSERT OVERWRITE TABLE sales PARTITION(dt='2024-01-01')\n" +
                "SELECT * FROM staging";

        SqlStatement stmt = parser.parse(sql);
        HiveInsertDetails details = (HiveInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOverwrite()).isTrue();
    }

    @Test
    @DisplayName("解析动态分区插入")
    void testDynamicPartitionInsert() {
        String sql = "INSERT OVERWRITE TABLE logs PARTITION(dt)\n" +
                "SELECT id, message, dt FROM staging";

        SqlStatement stmt = parser.parse(sql);
        HiveInsertDetails details = (HiveInsertDetails) stmt.getInsertDetails();
        assertThat(details.isDynamicPartition()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT INTO")
    void testInsertInto() {
        String sql = "INSERT INTO TABLE users\n" +
                "SELECT * FROM new_users";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析多插入")
    void testMultiInsert() {
        String sql = "FROM staging\n" +
                "INSERT OVERWRITE TABLE sales PARTITION(dt='2024-01-01') WHERE dt = '2024-01-01'\n" +
                "INSERT OVERWRITE TABLE sales PARTITION(dt='2024-01-02') WHERE dt = '2024-01-02'";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    // ==================== DDL 测试 ====================

    @Test
    @DisplayName("解析 ALTER TABLE ADD PARTITION")
    void testAlterTableAddPartition() {
        String sql = "ALTER TABLE sales ADD PARTITION (dt='2024-02-01') LOCATION '/user/hive/sales/dt=2024-02-01'";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE DROP PARTITION")
    void testAlterTableDropPartition() {
        String sql = "ALTER TABLE sales DROP IF EXISTS PARTITION (dt='2023-01-01')";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE RENAME")
    void testAlterTableRename() {
        String sql = "ALTER TABLE old_table RENAME TO new_table";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE CHANGE COLUMN")
    void testAlterTableChangeColumn() {
        String sql = "ALTER TABLE users CHANGE COLUMN old_name new_name STRING COMMENT 'New column name'";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD COLUMNS")
    void testAlterTableAddColumns() {
        String sql = "ALTER TABLE users ADD COLUMNS (phone STRING, address STRING)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE REPLACE COLUMNS")
    void testAlterTableReplaceColumns() {
        String sql = "ALTER TABLE users REPLACE COLUMNS (id INT, name STRING, email STRING)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE SET TBLPROPERTIES")
    void testAlterTableSetTblProperties() {
        String sql = "ALTER TABLE users SET TBLPROPERTIES ('comment' = 'User information table')";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE VIEW IF NOT EXISTS active_users AS\n" +
                "SELECT * FROM users WHERE status = 'active'";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 CREATE INDEX")
    void testCreateIndex() {
        String sql = "CREATE INDEX idx_users_name ON TABLE users (name) AS 'COMPACT'";

        SqlStatement stmt = parser.parse(sql);
        // CREATE INDEX parsing test
    }

    @Test
    @DisplayName("解析 MSCK REPAIR TABLE")
    void testMsckRepairTable() {
        String sql = "MSCK REPAIR TABLE sales";

        SqlStatement stmt = parser.parse(sql);
        // MSCK parsing test
    }

    @Test
    @DisplayName("解析 ANALYZE TABLE")
    void testAnalyzeTable() {
        String sql = "ANALYZE TABLE users COMPUTE STATISTICS FOR COLUMNS";

        SqlStatement stmt = parser.parse(sql);
        // ANALYZE parsing test
    }
}
