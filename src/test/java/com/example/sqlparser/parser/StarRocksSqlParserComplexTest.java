package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.starrocks.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * StarRocks SQL 解析器复杂场景测试
 */
class StarRocksSqlParserComplexTest {

    private StarRocksSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new StarRocksSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂 DUPLICATE KEY 表")
    void testComplexDuplicateKeyTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id BIGINT NOT NULL COMMENT '订单ID',\n" +
                "    user_id BIGINT NOT NULL COMMENT '用户ID',\n" +
                "    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',\n" +
                "    total_amount DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',\n" +
                "    status TINYINT NOT NULL DEFAULT '0' COMMENT '订单状态',\n" +
                "    pay_time DATETIME NULL COMMENT '支付时间',\n" +
                "    ship_time DATETIME NULL COMMENT '发货时间',\n" +
                "    receive_time DATETIME NULL COMMENT '收货时间',\n" +
                "    created_at DATETIME NOT NULL COMMENT '创建时间',\n" +
                "    updated_at DATETIME NOT NULL COMMENT '更新时间',\n" +
                "    deleted BOOLEAN NOT NULL DEFAULT '0' COMMENT '是否删除',\n" +
                "    metadata JSON NULL COMMENT '元数据',\n" +
                "    tags ARRAY<VARCHAR(50)> NULL COMMENT '标签'\n" +
                ")\n" +
                "DUPLICATE KEY(order_id, user_id, created_at)\n" +
                "PARTITION BY RANGE(created_at)\n" +
                "DISTRIBUTED BY HASH(user_id) BUCKETS 16\n" +
                "PROPERTIES (\n" +
                "    'replication_num' = '3',\n" +
                "    'storage_format' = 'DEFAULT',\n" +
                "    'enable_persistent_index' = 'true',\n" +
                "    'compression' = 'LZ4',\n" +
                "    'storage_medium' = 'SSD'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getTableType()).isEqualTo(StarRocksTableType.DUPLICATE_KEY);
        assertThat(table.getBucketInfo()).isNotNull();
        assertThat(table.getBucketInfo().getBucketCount()).isEqualTo(16);
    }

    @Test
    @DisplayName("解析复杂 AGGREGATE KEY 表")
    void testComplexAggregateKeyTable() {
        String sql = "CREATE TABLE sales_stats (\n" +
                "    date DATE NOT NULL,\n" +
                "    region VARCHAR(50) NOT NULL,\n" +
                "    product_id BIGINT NOT NULL,\n" +
                "    sales_count BIGINT SUM DEFAULT '0',\n" +
                "    sales_amount DECIMAL(18,2) SUM DEFAULT '0.00',\n" +
                "    unique_buyers HLL_UNION COMMENT 'UV',\n" +
                "    user_behavior BITMAP_UNION COMMENT '用户行为'\n" +
                ")\n" +
                "AGGREGATE KEY(date, region, product_id)\n" +
                "PARTITION BY RANGE(date)\n" +
                "DISTRIBUTED BY HASH(product_id) BUCKETS 12\n" +
                "PROPERTIES (\n" +
                "    'replication_num' = '3',\n" +
                "    'colocate_with' = 'sales_group'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_stats");
        assertThat(table.getTableType()).isEqualTo(StarRocksTableType.AGGREGATE_KEY);
    }

    @Test
    @DisplayName("解析复杂 UNIQUE KEY 表")
    void testComplexUniqueKeyTable() {
        String sql = "CREATE TABLE user_profiles (\n" +
                "    user_id BIGINT NOT NULL COMMENT '用户ID',\n" +
                "    username VARCHAR(50) NOT NULL COMMENT '用户名',\n" +
                "    email VARCHAR(100) NOT NULL COMMENT '邮箱',\n" +
                "    phone VARCHAR(20) NULL COMMENT '电话',\n" +
                "    avatar_url VARCHAR(500) NULL COMMENT '头像URL',\n" +
                "    level INT NULL DEFAULT '1' COMMENT '等级',\n" +
                "    vip_expire_time DATETIME NULL COMMENT 'VIP过期时间',\n" +
                "    created_at DATETIME NOT NULL COMMENT '创建时间',\n" +
                "    updated_at DATETIME NOT NULL COMMENT '更新时间'\n" +
                ")\n" +
                "UNIQUE KEY(user_id)\n" +
                "DISTRIBUTED BY HASH(user_id) BUCKETS 8\n" +
                "PROPERTIES (\n" +
                "    'replication_num' = '3',\n" +
                "    'enable_unique_key_merge_on_write' = 'true'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("user_profiles");
        assertThat(table.getTableType()).isEqualTo(StarRocksTableType.UNIQUE_KEY);
    }

    @Test
    @DisplayName("解析复杂 PRIMARY KEY 表")
    void testComplexPrimaryKeyTable() {
        String sql = "CREATE TABLE orders_primary (\n" +
                "    order_id BIGINT NOT NULL COMMENT '订单ID',\n" +
                "    order_no VARCHAR(32) NOT NULL COMMENT '订单编号',\n" +
                "    user_id BIGINT NOT NULL COMMENT '用户ID',\n" +
                "    total_amount DECIMAL(18,2) NOT NULL COMMENT '总金额',\n" +
                "    status TINYINT NOT NULL COMMENT '状态',\n" +
                "    created_at DATETIME NOT NULL COMMENT '创建时间'\n" +
                ")\n" +
                "PRIMARY KEY(order_id)\n" +
                "DISTRIBUTED BY HASH(order_id) BUCKETS 16\n" +
                "PROPERTIES (\n" +
                "    'replication_num' = '3',\n" +
                "    'enable_persistent_index' = 'true'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders_primary");
        assertThat(table.getTableType()).isEqualTo(StarRocksTableType.PRIMARY_KEY);
    }

    @Test
    @DisplayName("解析 RANGE 分区表带具体分区")
    void testRangePartitionWithPartitions() {
        String sql = "CREATE TABLE events_range (\n" +
                "    event_id BIGINT NOT NULL,\n" +
                "    event_time DATETIME NOT NULL,\n" +
                "    user_id BIGINT,\n" +
                "    event_type VARCHAR(50)\n" +
                ")\n" +
                "DUPLICATE KEY(event_id, event_time)\n" +
                "PARTITION BY RANGE(event_time) (\n" +
                "    PARTITION p202401 VALUES LESS THAN ('2024-02-01'),\n" +
                "    PARTITION p202402 VALUES LESS THAN ('2024-03-01'),\n" +
                "    PARTITION p202403 VALUES LESS THAN ('2024-04-01'),\n" +
                "    PARTITION p202404 VALUES LESS THAN ('2024-05-01'),\n" +
                "    PARTITION p_future VALUES LESS THAN (MAXVALUE)\n" +
                ")\n" +
                "DISTRIBUTED BY HASH(event_id) BUCKETS 10";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("events_range");
        assertThat(table.getPartitionInfo()).isNotNull();
        assertThat(table.getPartitionInfo().getType()).isEqualTo(PartitionType.RANGE);
    }

    @Test
    @DisplayName("解析 LIST 分区表")
    void testListPartitionTable() {
        String sql = "CREATE TABLE sales_by_region (\n" +
                "    sale_id BIGINT NOT NULL,\n" +
                "    region VARCHAR(20) NOT NULL,\n" +
                "    amount DECIMAL(18,2)\n" +
                ")\n" +
                "DUPLICATE KEY(sale_id)\n" +
                "PARTITION BY LIST(region) (\n" +
                "    PARTITION p_north VALUES IN ('North', 'Northeast', 'Northwest'),\n" +
                "    PARTITION p_south VALUES IN ('South', 'Southeast', 'Southwest'),\n" +
                "    PARTITION p_east VALUES IN ('East'),\n" +
                "    PARTITION p_west VALUES IN ('West')\n" +
                ")\n" +
                "DISTRIBUTED BY HASH(sale_id) BUCKETS 8";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_by_region");
        assertThat(table.getPartitionInfo().getType()).isEqualTo(PartitionType.LIST);
    }

    @Test
    @DisplayName("解析自动分桶表")
    void testAutoBucketTable() {
        String sql = "CREATE TABLE auto_bucket_table (\n" +
                "    id BIGINT NOT NULL,\n" +
                "    name VARCHAR(100)\n" +
                ")\n" +
                "DUPLICATE KEY(id)\n" +
                "DISTRIBUTED BY HASH(id) BUCKETS AUTO\n" +
                "PROPERTIES ('bucket_size' = '1073741824')";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getBucketInfo()).isNotNull();
        assertThat(table.getBucketInfo().isAutoBucket()).isTrue();
    }

    @Test
    @DisplayName("解析随机分桶表")
    void testRandomBucketTable() {
        String sql = "CREATE TABLE random_bucket_table (\n" +
                "    id BIGINT NOT NULL,\n" +
                "    data STRING\n" +
                ")\n" +
                "DUPLICATE KEY(id)\n" +
                "DISTRIBUTED BY RANDOM BUCKETS 16";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("random_bucket_table");
    }

    @Test
    @DisplayName("解析外部表 - Hive")
    void testExternalHiveTable() {
        String sql = "CREATE EXTERNAL TABLE hive_external_table (\n" +
                "    id BIGINT,\n" +
                "    name VARCHAR(100)\n" +
                ")\n" +
                "ENGINE=HIVE\n" +
                "PROPERTIES (\n" +
                "    'hive.metastore.uris' = 'thrift://localhost:9083',\n" +
                "    'database' = 'default',\n" +
                "    'table' = 'hive_table'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析外部表 - Iceberg")
    void testExternalIcebergTable() {
        String sql = "CREATE EXTERNAL TABLE iceberg_external_table (\n" +
                "    id BIGINT,\n" +
                "    data STRING\n" +
                ")\n" +
                "ENGINE=ICEBERG\n" +
                "PROPERTIES (\n" +
                "    'iceberg.catalog.type' = 'HIVE',\n" +
                "    'iceberg.catalog.hive.metastore.uris' = 'thrift://localhost:9083'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析外部表 - Hudi")
    void testExternalHudiTable() {
        String sql = "CREATE EXTERNAL TABLE hudi_external_table (\n" +
                "    id BIGINT,\n" +
                "    name VARCHAR(100)\n" +
                ")\n" +
                "ENGINE=HUDI\n" +
                "PROPERTIES (\n" +
                "    'hive.metastore.uris' = 'thrift://localhost:9083'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析外部表 - Delta Lake")
    void testExternalDeltaTable() {
        String sql = "CREATE EXTERNAL TABLE delta_external_table (\n" +
                "    id BIGINT,\n" +
                "    data STRING\n" +
                ")\n" +
                "ENGINE=DELTALAKE\n" +
                "PROPERTIES (\n" +
                "    'hive.metastore.uris' = 'thrift://localhost:9083'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析 JDBC 外部表")
    void testJdbcExternalTable() {
        String sql = "CREATE EXTERNAL TABLE jdbc_external_table (\n" +
                "    id BIGINT,\n" +
                "    name VARCHAR(100)\n" +
                ")\n" +
                "ENGINE=JDBC\n" +
                "PROPERTIES (\n" +
                "    'resource' = 'jdbc_resource',\n" +
                "    'table' = 'remote_table',\n" +
                "    'table_type' = 'mysql'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析 Elasticsearch 外部表")
    void testElasticsearchExternalTable() {
        String sql = "CREATE EXTERNAL TABLE es_external_table (\n" +
                "    id BIGINT,\n" +
                "    content STRING\n" +
                ")\n" +
                "ENGINE=ELASTICSEARCH\n" +
                "PROPERTIES (\n" +
                "    'hosts' = 'http://localhost:9200',\n" +
                "    'index' = 'test_index',\n" +
                "    'type' = '_doc'\n" +
                ")";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.isExternal()).isTrue();
    }

    @Test
    @DisplayName("解析复杂数据类型表")
    void testComplexDataTypesTable() {
        String sql = "CREATE TABLE complex_types (\n" +
                "    id BIGINT NOT NULL,\n" +
                "    name VARCHAR(100),\n" +
                "    price DECIMAL(18,2),\n" +
                "    tags ARRAY<VARCHAR(50)>,\n" +
                "    properties MAP<STRING, STRING>,\n" +
                "    info JSON,\n" +
                "    binary_data VARBINARY\n" +
                ")\n" +
                "DUPLICATE KEY(id)\n" +
                "DISTRIBUTED BY HASH(id) BUCKETS 4";

        StarRocksCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("complex_types");
    }

    @Test
    @DisplayName("解析物化视图")
    void testMaterializedView() {
        String sql = "CREATE MATERIALIZED VIEW mv_orders_by_day\n" +
                "PARTITION BY date_trunc('day', created_at)\n" +
                "DISTRIBUTED BY HASH(user_id) BUCKETS 8\n" +
                "REFRESH ASYNC START('2024-01-01') EVERY (INTERVAL 1 HOUR)\n" +
                "AS SELECT\n" +
                "    date_trunc('day', created_at) AS day,\n" +
                "    user_id,\n" +
                "    count(*) AS order_count,\n" +
                "    sum(total_amount) AS total_revenue\n" +
                "FROM orders\n" +
                "GROUP BY date_trunc('day', created_at), user_id";

        SqlStatement stmt = parser.parse(sql);
        // CREATE MATERIALIZED VIEW parsing test
    }

    @Test
    @DisplayName("解析同步物化视图")
    void testSyncMaterializedView() {
        String sql = "CREATE MATERIALIZED VIEW mv_user_order_count AS\n" +
                "SELECT user_id, count(*) as order_count\n" +
                "FROM orders\n" +
                "GROUP BY user_id";

        SqlStatement stmt = parser.parse(sql);
        // CREATE MATERIALIZED VIEW parsing test
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
                "    LEAD(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date) AS next_amount\n" +
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
                "    NDV(user_id) AS approx_unique_users,\n" +
                "    BITMAP_UNION_COUNT(user_id) AS bitmap_unique_users\n" +
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

    @Test
    @DisplayName("解析 QUALIFY 查询")
    void testQualifyQuery() {
        String sql = "SELECT\n" +
                "    user_id,\n" +
                "    order_date,\n" +
                "    amount,\n" +
                "    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY order_date DESC) AS rn\n" +
                "FROM orders\n" +
                "QUALIFY rn <= 3";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    // ==================== INSERT 测试 ====================

    @Test
    @DisplayName("解析 INSERT OVERWRITE")
    void testInsertOverwrite() {
        String sql = "INSERT OVERWRITE sales PARTITION(p202401)\n" +
                "SELECT * FROM staging";

        SqlStatement stmt = parser.parse(sql);
        StarRocksInsertDetails details = (StarRocksInsertDetails) stmt.getInsertDetails();
        assertThat(details.isOverwrite()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT INTO 多分区")
    void testInsertIntoPartitions() {
        String sql = "INSERT INTO sales PARTITION(p202401, p202402, p202403)\n" +
                "VALUES (1, 100.0), (2, 200.0)";

        SqlStatement stmt = parser.parse(sql);
        StarRocksInsertDetails details = (StarRocksInsertDetails) stmt.getInsertDetails();
        assertThat(details.getTargetPartitions()).hasSize(3);
    }

    @Test
    @DisplayName("解析 INSERT INTO 指定列")
    void testInsertIntoColumns() {
        String sql = "INSERT INTO users (id, name, email)\n" +
                "VALUES (1, 'John', 'john@example.com')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT SELECT")
    void testInsertSelect() {
        String sql = "INSERT INTO orders_archive\n" +
                "SELECT * FROM orders\n" +
                "WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    // ==================== DDL 测试 ====================

    @Test
    @DisplayName("解析 ALTER TABLE ADD PARTITION")
    void testAlterTableAddPartition() {
        String sql = "ALTER TABLE sales ADD PARTITION p202404\n" +
                "VALUES LESS THAN ('2024-05-01')";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE DROP PARTITION")
    void testAlterTableDropPartition() {
        String sql = "ALTER TABLE sales DROP PARTITION p202401";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE MODIFY COLUMN")
    void testAlterTableModifyColumn() {
        String sql = "ALTER TABLE users MODIFY COLUMN name VARCHAR(200)";

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

    @Test
    @DisplayName("解析 ALTER TABLE DROP COLUMN")
    void testAlterTableDropColumn() {
        String sql = "ALTER TABLE users DROP COLUMN old_field";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE RENAME")
    void testAlterTableRename() {
        String sql = "ALTER TABLE old_table RENAME new_table";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE VIEW active_users AS\n" +
                "SELECT * FROM users WHERE status = 'active'";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 ANALYZE TABLE")
    void testAnalyzeTable() {
        String sql = "ANALYZE TABLE users";

        SqlStatement stmt = parser.parse(sql);
        // ANALYZE parsing test
    }

    @Test
    @DisplayName("解析 REFRESH EXTERNAL TABLE")
    void testRefreshExternalTable() {
        String sql = "REFRESH EXTERNAL TABLE hive_external_table";

        SqlStatement stmt = parser.parse(sql);
        // REFRESH parsing test
    }
}
