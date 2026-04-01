package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.clickhouse.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ClickHouse SQL 解析器复杂场景测试
 */
class ClickHouseSqlParserComplexTest {

    private ClickHouseSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new ClickHouseSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂 MergeTree 表")
    void testComplexMergeTreeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ecommerce.orders (\n" +
                "    order_id UInt64,\n" +
                "    user_id UInt64,\n" +
                "    order_no String,\n" +
                "    total_amount Decimal(18, 2),\n" +
                "    status UInt8,\n" +
                "    pay_time Nullable(DateTime),\n" +
                "    ship_time Nullable(DateTime),\n" +
                "    receive_time Nullable(DateTime),\n" +
                "    created_at DateTime,\n" +
                "    updated_at DateTime,\n" +
                "    deleted UInt8 DEFAULT 0,\n" +
                "    metadata String DEFAULT '{}',\n" +
                "    tags Array(String) DEFAULT [],\n" +
                "    items Nested (\n" +
                "        product_id UInt64,\n" +
                "        quantity UInt32,\n" +
                "        price Decimal(18, 2)\n" +
                "    ),\n" +
                "    INDEX idx_order_no order_no TYPE bloom_filter GRANULARITY 3,\n" +
                "    INDEX idx_user_id user_id TYPE minmax GRANULARITY 4,\n" +
                "    INDEX idx_metadata metadata TYPE tokenbf_v1(512, 3, 0) GRANULARITY 1\n" +
                ") ENGINE = MergeTree()\n" +
                "PARTITION BY toYYYYMM(created_at)\n" +
                "ORDER BY (user_id, created_at, order_id)\n" +
                "PRIMARY KEY (user_id, created_at)\n" +
                "SAMPLE BY order_id\n" +
                "TTL created_at + INTERVAL 2 YEAR TO VOLUME 'cold',\n" +
                "    created_at + INTERVAL 5 YEAR DELETE\n" +
                "SETTINGS index_granularity = 8192,\n" +
                "    storage_policy = 'hot_cold_separation'";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getEngine()).isEqualTo("MergeTree");
        assertThat(table.getPartitionBy()).isEqualTo("toYYYYMM(created_at)");
        assertThat(table.getOrderBy()).isEqualTo("(user_id, created_at, order_id)");
        assertThat(table.getPrimaryKey()).isEqualTo("(user_id, created_at)");
        assertThat(table.getSampleBy()).isEqualTo("order_id");
        assertThat(table.getTtl()).isNotEmpty();
        assertThat(table.getSettings()).isNotEmpty();
    }

    @Test
    @DisplayName("解析 ReplacingMergeTree 表")
    void testReplacingMergeTreeTable() {
        String sql = "CREATE TABLE user_profiles (\n" +
                "    user_id UInt64,\n" +
                "    version UInt64,\n" +
                "    name String,\n" +
                "    email String,\n" +
                "    updated_at DateTime\n" +
                ") ENGINE = ReplacingMergeTree(version)\n" +
                "PARTITION BY toYYYYMM(updated_at)\n" +
                "ORDER BY user_id\n" +
                "SETTINGS index_granularity = 8192";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("ReplacingMergeTree");
    }

    @Test
    @DisplayName("解析 SummingMergeTree 表")
    void testSummingMergeTreeTable() {
        String sql = "CREATE TABLE daily_stats (\n" +
                "    date Date,\n" +
                "    user_id UInt64,\n" +
                "    impressions UInt64,\n" +
                "    clicks UInt64,\n" +
                "    revenue Decimal(18, 2)\n" +
                ") ENGINE = SummingMergeTree(impressions, clicks, revenue)\n" +
                "PARTITION BY toYYYYMM(date)\n" +
                "ORDER BY (date, user_id)";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("SummingMergeTree");
    }

    @Test
    @DisplayName("解析 AggregatingMergeTree 表")
    void testAggregatingMergeTreeTable() {
        String sql = "CREATE TABLE metrics_aggregated (\n" +
                "    timestamp DateTime,\n" +
                "    service String,\n" +
                "    metric_name String,\n" +
                "    value_sum AggregateFunction(sum, Float64),\n" +
                "    value_count AggregateFunction(count, UInt64),\n" +
                "    value_avg AggregateFunction(avg, Float64),\n" +
                "    value_quantiles AggregateFunction(quantiles(0.5, 0.9, 0.99), Float64)\n" +
                ") ENGINE = AggregatingMergeTree()\n" +
                "PARTITION BY toYYYYMMDD(timestamp)\n" +
                "ORDER BY (service, metric_name, timestamp)";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("AggregatingMergeTree");
    }

    @Test
    @DisplayName("解析 CollapsingMergeTree 表")
    void testCollapsingMergeTreeTable() {
        String sql = "CREATE TABLE user_events (\n" +
                "    user_id UInt64,\n" +
                "    event_time DateTime,\n" +
                "    event_type String,\n" +
                "    sign Int8\n" +
                ") ENGINE = CollapsingMergeTree(sign)\n" +
                "ORDER BY (user_id, event_time, event_type)";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("CollapsingMergeTree");
    }

    @Test
    @DisplayName("解析 VersionedCollapsingMergeTree 表")
    void testVersionedCollapsingMergeTreeTable() {
        String sql = "CREATE TABLE user_events_versioned (\n" +
                "    user_id UInt64,\n" +
                "    event_time DateTime,\n" +
                "    event_type String,\n" +
                "    sign Int8,\n" +
                "    version UInt64\n" +
                ") ENGINE = VersionedCollapsingMergeTree(sign, version)\n" +
                "ORDER BY (user_id, event_time, event_type)";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("VersionedCollapsingMergeTree");
    }

    @Test
    @DisplayName("解析外部表 - JDBC")
    void testExternalTableJDBC() {
        String sql = "CREATE TABLE jdbc_table (\n" +
                "    id UInt64,\n" +
                "    name String\n" +
                ") ENGINE = JDBC('jdbc:postgresql://host:5432/database', 'schema', 'table')";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("JDBC");
    }

    @Test
    @DisplayName("解析外部表 - MySQL")
    void testExternalTableMySQL() {
        String sql = "CREATE TABLE mysql_table (\n" +
                "    id UInt64,\n" +
                "    name String\n" +
                ") ENGINE = MySQL('host:3306', 'database', 'table', 'user', 'password')";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("MySQL");
    }

    @Test
    @DisplayName("解析外部表 - Kafka")
    void testExternalTableKafka() {
        String sql = "CREATE TABLE kafka_queue (\n" +
                "    timestamp UInt64,\n" +
                "    level String,\n" +
                "    message String\n" +
                ") ENGINE = Kafka()\n" +
                "SETTINGS\n" +
                "    kafka_broker_list = 'localhost:9092',\n" +
                "    kafka_topic_list = 'logs',\n" +
                "    kafka_group_name = 'clickhouse',\n" +
                "    kafka_format = 'JSONEachRow'";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getEngine()).isEqualTo("Kafka");
    }

    @Test
    @DisplayName("解析带所有数据类型的表")
    void testTableWithAllDataTypes() {
        String sql = "CREATE TABLE all_types (\n" +
                "    int8_col Int8,\n" +
                "    int16_col Int16,\n" +
                "    int32_col Int32,\n" +
                "    int64_col Int64,\n" +
                "    uint8_col UInt8,\n" +
                "    uint16_col UInt16,\n" +
                "    uint32_col UInt32,\n" +
                "    uint64_col UInt64,\n" +
                "    float32_col Float32,\n" +
                "    float64_col Float64,\n" +
                "    decimal32_col Decimal(9, 2),\n" +
                "    decimal64_col Decimal(18, 4),\n" +
                "    string_col String,\n" +
                "    fixed_string_col FixedString(100),\n" +
                "    date_col Date,\n" +
                "    datetime_col DateTime,\n" +
                "    datetime64_col DateTime64(3),\n" +
                "    uuid_col UUID,\n" +
                "    enum8_col Enum8('hello' = 1, 'world' = 2),\n" +
                "    low_cardinality_col LowCardinality(String),\n" +
                "    nullable_col Nullable(Int32),\n" +
                "    array_col Array(String),\n" +
                "    tuple_col Tuple(Int32, String, Float64),\n" +
                "    map_col Map(String, Int32),\n" +
                "    ipv4_col IPv4,\n" +
                "    ipv6_col IPv6\n" +
                ") ENGINE = MergeTree()\n" +
                "ORDER BY int64_col";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("all_types");
    }

    @Test
    @DisplayName("解析带 CODEC 的列")
    void testColumnsWithCodec() {
        String sql = "CREATE TABLE codec_test (\n" +
                "    id UInt64,\n" +
                "    raw_data String CODEC(ZSTD(1)),\n" +
                "    compressed_data String CODEC(LZ4),\n" +
                "    delta_compressed UInt64 CODEC(Delta, LZ4),\n" +
                "    gorilla_compressed Float64 CODEC(Gorilla, LZ4)\n" +
                ") ENGINE = MergeTree()\n" +
                "ORDER BY id";

        ClickHouseCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("codec_test");
    }

    @Test
    @DisplayName("解析带物化视图的表")
    void testMaterializedView() {
        String sql = "CREATE MATERIALIZED VIEW mv_orders_by_day\n" +
                "ENGINE = SummingMergeTree()\n" +
                "PARTITION BY toYYYYMM(day)\n" +
                "ORDER BY day\n" +
                "AS SELECT\n" +
                "    toDate(created_at) AS day,\n" +
                "    count() AS order_count,\n" +
                "    sum(total_amount) AS total_revenue\n" +
                "FROM orders\n" +
                "GROUP BY toDate(created_at)";

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
                "ALL INNER JOIN orders o ON u.user_id = o.user_id\n" +
                "LEFT JOIN order_items oi ON o.order_id = oi.order_id\n" +
                "GLOBAL LEFT JOIN products p ON oi.product_id = p.product_id\n" +
                "PREWHERE o.status = 1\n" +
                "WHERE o.created_at >= '2024-01-01'\n" +
                "ORDER BY o.created_at DESC\n" +
                "LIMIT 100 BY u.user_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 ARRAY JOIN 查询")
    void testArrayJoinQuery() {
        String sql = "SELECT\n" +
                "    order_id,\n" +
                "    item.product_id,\n" +
                "    item.quantity,\n" +
                "    item.price\n" +
                "FROM orders\n" +
                "ARRAY JOIN items AS item\n" +
                "WHERE item.quantity > 0";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 SAMPLE 查询")
    void testSampleQuery() {
        String sql = "SELECT\n" +
                "    user_id,\n" +
                "    avg(total_amount)\n" +
                "FROM orders\n" +
                "SAMPLE 0.1\n" +
                "GROUP BY user_id";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.getSample()).isNotNull();
    }

    @Test
    @DisplayName("解析 FINAL 查询")
    void testFinalQuery() {
        String sql = "SELECT * FROM orders FINAL WHERE user_id = 123";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.isFinalModifier()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH ROLLUP")
    void testGroupByWithRollup() {
        String sql = "SELECT\n" +
                "    year,\n" +
                "    month,\n" +
                "    region,\n" +
                "    sum(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH ROLLUP";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.isWithRollup()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH CUBE")
    void testGroupByWithCube() {
        String sql = "SELECT\n" +
                "    year,\n" +
                "    month,\n" +
                "    region,\n" +
                "    sum(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH CUBE";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.isWithCube()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH TOTALS")
    void testGroupByWithTotals() {
        String sql = "SELECT\n" +
                "    region,\n" +
                "    sum(sales)\n" +
                "FROM sales_data\n" +
                "GROUP BY region WITH TOTALS";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.isWithTotals()).isTrue();
    }

    @Test
    @DisplayName("解析 LIMIT BY 查询")
    void testLimitByQuery() {
        String sql = "SELECT\n" +
                "    user_id,\n" +
                "    order_id,\n" +
                "    total_amount\n" +
                "FROM orders\n" +
                "ORDER BY user_id, total_amount DESC\n" +
                "LIMIT 3 BY user_id";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 LIMIT WITH TIES")
    void testLimitWithTies() {
        String sql = "SELECT\n" +
                "    user_id,\n" +
                "    score\n" +
                "FROM scores\n" +
                "ORDER BY score DESC\n" +
                "LIMIT 10 WITH TIES";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.isWithTies()).isTrue();
    }

    @Test
    @DisplayName("解析 FORMAT 查询")
    void testFormatQuery() {
        String sql = "SELECT * FROM users FORMAT JSON";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseSelectDetails details = (ClickHouseSelectDetails) stmt.getSelectDetails();
        assertThat(details.getFormat()).isEqualTo("JSON");
    }

    @Test
    @DisplayName("解析复杂聚合查询")
    void testComplexAggregation() {
        String sql = "SELECT\n" +
                "    toDate(created_at) AS day,\n" +
                "    count() AS total_orders,\n" +
                "    uniqExact(user_id) AS unique_users,\n" +
                "    sum(total_amount) AS total_revenue,\n" +
                "    avg(total_amount) AS avg_order_value,\n" +
                "    quantile(0.5)(total_amount) AS median_amount,\n" +
                "    quantiles(0.25, 0.5, 0.75, 0.9, 0.99)(total_amount) AS amount_percentiles,\n" +
                "    argMax(total_amount, created_at) AS last_order_amount,\n" +
                "    groupArray(10)(order_id) AS recent_order_ids\n" +
                "FROM orders\n" +
                "WHERE status = 1\n" +
                "GROUP BY toDate(created_at)\n" +
                "HAVING count() >= 10\n" +
                "ORDER BY day DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    // ==================== INSERT 测试 ====================

    @Test
    @DisplayName("解析 INSERT VALUES")
    void testInsertValues() {
        String sql = "INSERT INTO users (id, name, email) VALUES (1, 'John', 'john@example.com')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT SELECT")
    void testInsertSelect() {
        String sql = "INSERT INTO orders_archive\n" +
                "SELECT * FROM orders\n" +
                "WHERE created_at < today() - INTERVAL 1 YEAR";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT FORMAT")
    void testInsertFormat() {
        String sql = "INSERT INTO users (id, name) FORMAT CSV";

        SqlStatement stmt = parser.parse(sql);
        ClickHouseInsertDetails details = (ClickHouseInsertDetails) stmt.getInsertDetails();
        assertThat(details.getFormat()).isEqualTo("CSV");
    }
}
