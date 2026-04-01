package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.mysql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MySQL SQL 解析器复杂场景测试
 */
class MySQLSqlParserComplexTest {

    private MySQLSqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new MySQLSqlParser();
    }

    // ==================== 复杂 CREATE TABLE 测试 ====================

    @Test
    @DisplayName("解析复杂电商订单表")
    void testComplexEcommerceTable() {
        String sql = "CREATE TABLE IF NOT EXISTS `ecommerce`.`orders` (\n" +
                "    `order_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',\n" +
                "    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',\n" +
                "    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',\n" +
                "    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',\n" +
                "    `status` TINYINT UNSIGNED NOT NULL DEFAULT '0' COMMENT '订单状态:0-待支付,1-已支付,2-已发货,3-已完成,4-已取消',\n" +
                "    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',\n" +
                "    `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',\n" +
                "    `receive_time` DATETIME DEFAULT NULL COMMENT '收货时间',\n" +
                "    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                "    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',\n" +
                "    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '是否删除:0-否,1-是',\n" +
                "    PRIMARY KEY (`order_id`),\n" +
                "    UNIQUE KEY `uk_order_no` (`order_no`),\n" +
                "    KEY `idx_user_id` (`user_id`),\n" +
                "    KEY `idx_status_created` (`status`, `created_at`),\n" +
                "    KEY `idx_created_at` (`created_at`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表' " +
                "AUTO_INCREMENT=1000000 ROW_FORMAT=DYNAMIC;";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("orders");
        assertThat(table.getEngine()).isEqualTo("InnoDB");
        assertThat(table.getCharset()).isEqualTo("utf8mb4");
        assertThat(table.getCollation()).isEqualTo("utf8mb4_unicode_ci");
        assertThat(table.getAutoIncrement()).isEqualTo(1000000);
    }

    @Test
    @DisplayName("解析带所有索引类型的表")
    void testTableWithAllIndexTypes() {
        String sql = "CREATE TABLE index_test (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    unique_col VARCHAR(100),\n" +
                "    index_col1 VARCHAR(50),\n" +
                "    index_col2 INT,\n" +
                "    fulltext_col TEXT,\n" +
                "    spatial_col POINT,\n" +
                "    UNIQUE KEY uk_unique (unique_col),\n" +
                "    INDEX idx_normal (index_col1),\n" +
                "    KEY idx_key (index_col2),\n" +
                "    FULLTEXT INDEX ft_idx (fulltext_col),\n" +
                "    SPATIAL INDEX sp_idx (spatial_col),\n" +
                "    INDEX idx_composite (index_col1, index_col2) USING BTREE COMMENT '复合索引'\n" +
                ") ENGINE=InnoDB";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("index_test");
        assertThat(table.getIndexes()).isNotEmpty();
    }

    @Test
    @DisplayName("解析带生成列的表")
    void testTableWithGeneratedColumns() {
        String sql = "CREATE TABLE generated_cols (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    first_name VARCHAR(50),\n" +
                "    last_name VARCHAR(50),\n" +
                "    full_name VARCHAR(101) AS (CONCAT(first_name, ' ', last_name)) STORED,\n" +
                "    birth_date DATE,\n" +
                "    age INT AS (TIMESTAMPDIFF(YEAR, birth_date, CURDATE())) VIRTUAL\n" +
                ") ENGINE=InnoDB";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("generated_cols");
    }

    @Test
    @DisplayName("解析带外键约束的表")
    void testTableWithForeignKeys() {
        String sql = "CREATE TABLE order_items (\n" +
                "    item_id INT PRIMARY KEY AUTO_INCREMENT,\n" +
                "    order_id BIGINT UNSIGNED NOT NULL,\n" +
                "    product_id INT NOT NULL,\n" +
                "    quantity INT NOT NULL DEFAULT 1,\n" +
                "    price DECIMAL(18,2) NOT NULL,\n" +
                "    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE ON UPDATE CASCADE,\n" +
                "    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT\n" +
                ") ENGINE=InnoDB";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("order_items");
    }

    @Test
    @DisplayName("解析带 CHECK 约束的表 (MySQL 8.0+)")
    void testTableWithCheckConstraints() {
        String sql = "CREATE TABLE check_test (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    age INT,\n" +
                "    email VARCHAR(100),\n" +
                "    status ENUM('active', 'inactive', 'pending'),\n" +
                "    CONSTRAINT chk_age CHECK (age >= 0 AND age <= 150),\n" +
                "    CONSTRAINT chk_email CHECK (email LIKE '%@%')\n" +
                ") ENGINE=InnoDB";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("check_test");
    }

    @Test
    @DisplayName("解析分区表 - RANGE")
    void testRangePartitionedTable() {
        String sql = "CREATE TABLE sales_range (\n" +
                "    id INT,\n" +
                "    sale_date DATE,\n" +
                "    amount DECIMAL(10,2)\n" +
                ") PARTITION BY RANGE (YEAR(sale_date)) (\n" +
                "    PARTITION p2021 VALUES LESS THAN (2022),\n" +
                "    PARTITION p2022 VALUES LESS THAN (2023),\n" +
                "    PARTITION p2023 VALUES LESS THAN (2024),\n" +
                "    PARTITION p_future VALUES LESS THAN MAXVALUE\n" +
                ")";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_range");
    }

    @Test
    @DisplayName("解析分区表 - LIST")
    void testListPartitionedTable() {
        String sql = "CREATE TABLE sales_list (\n" +
                "    id INT,\n" +
                "    region VARCHAR(20),\n" +
                "    amount DECIMAL(10,2)\n" +
                ") PARTITION BY LIST COLUMNS(region) (\n" +
                "    PARTITION p_north VALUES IN ('北京', '天津', '河北'),\n" +
                "    PARTITION p_east VALUES IN ('上海', '江苏', '浙江'),\n" +
                "    PARTITION p_south VALUES IN ('广东', '福建', '海南')\n" +
                ")";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_list");
    }

    @Test
    @DisplayName("解析分区表 - HASH")
    void testHashPartitionedTable() {
        String sql = "CREATE TABLE sales_hash (\n" +
                "    id INT,\n" +
                "    customer_id INT\n" +
                ") PARTITION BY HASH(customer_id) PARTITIONS 8";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_hash");
    }

    @Test
    @DisplayName("解析分区表 - KEY")
    void testKeyPartitionedTable() {
        String sql = "CREATE TABLE sales_key (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    created_at TIMESTAMP\n" +
                ") PARTITION BY KEY() PARTITIONS 4";

        MySQLCreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("sales_key");
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
                "WHERE o.status = 1\n" +
                "    AND o.created_at >= '2024-01-01'\n" +
                "ORDER BY o.created_at DESC\n" +
                "LIMIT 100";

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
    @DisplayName("解析相关子查询")
    void testCorrelatedSubQuery() {
        String sql = "SELECT u.* FROM users u WHERE EXISTS (\n" +
                "    SELECT 1 FROM orders o WHERE o.user_id = u.user_id AND o.status = 1\n" +
                ")";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UNION 查询")
    void testUnionQuery() {
        String sql = "SELECT id, name FROM table1 WHERE status = 1\n" +
                "UNION\n" +
                "SELECT id, name FROM table2 WHERE status = 1\n" +
                "ORDER BY name\n" +
                "LIMIT 50";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 UNION ALL 查询")
    void testUnionAllQuery() {
        String sql = "SELECT * FROM logs_202401\n" +
                "UNION ALL\n" +
                "SELECT * FROM logs_202402\n" +
                "UNION ALL\n" +
                "SELECT * FROM logs_202403";

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
                "    SUM(amount) OVER (PARTITION BY user_id ORDER BY order_date) AS running_total,\n" +
                "    AVG(amount) OVER (PARTITION BY user_id) AS avg_amount,\n" +
                "    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY order_date DESC) AS rn,\n" +
                "    RANK() OVER (ORDER BY amount DESC) AS rank_amount,\n" +
                "    LAG(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date) AS prev_amount,\n" +
                "    LEAD(amount, 1) OVER (PARTITION BY user_id ORDER BY order_date) AS next_amount\n" +
                "FROM orders";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 CTE 递归查询")
    void testRecursiveCTE() {
        String sql = "WITH RECURSIVE category_tree AS (\n" +
                "    SELECT id, name, parent_id, 0 AS level, CAST(id AS CHAR(255)) AS path\n" +
                "    FROM categories WHERE parent_id IS NULL\n" +
                "    UNION ALL\n" +
                "    SELECT c.id, c.name, c.parent_id, ct.level + 1, CONCAT(ct.path, ',', c.id)\n" +
                "    FROM categories c\n" +
                "    INNER JOIN category_tree ct ON c.parent_id = ct.id\n" +
                ")\n" +
                "SELECT * FROM category_tree ORDER BY path";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析 GROUP BY WITH ROLLUP")
    void testGroupByWithRollup() {
        String sql = "SELECT year, month, region, SUM(sales) AS total_sales\n" +
                "FROM sales_data\n" +
                "GROUP BY year, month, region WITH ROLLUP";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析复杂聚合查询")
    void testComplexAggregation() {
        String sql = "SELECT\n" +
                "    DATE(created_at) AS date,\n" +
                "    COUNT(*) AS total_orders,\n" +
                "    COUNT(DISTINCT user_id) AS unique_users,\n" +
                "    SUM(total_amount) AS total_revenue,\n" +
                "    AVG(total_amount) AS avg_order_value,\n" +
                "    MAX(total_amount) AS max_order,\n" +
                "    MIN(total_amount) AS min_order,\n" +
                "    STDDEV(total_amount) AS stddev_amount\n" +
                "FROM orders\n" +
                "WHERE status = 1\n" +
                "GROUP BY DATE(created_at)\n" +
                "HAVING COUNT(*) >= 10\n" +
                "ORDER BY date DESC";

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
                "    ('user4', 'user4@test.com', 28)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT ... SELECT")
    void testInsertSelect() {
        String sql = "INSERT INTO orders_archive (order_id, user_id, total_amount, created_at)\n" +
                "SELECT order_id, user_id, total_amount, created_at\n" +
                "FROM orders\n" +
                "WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    @Test
    @DisplayName("解析 INSERT DELAYED")
    void testInsertDelayed() {
        String sql = "INSERT DELAYED INTO logs (level, message) VALUES ('INFO', 'System started')";

        SqlStatement stmt = parser.parse(sql);
        MySQLInsertDetails details = (MySQLInsertDetails) stmt.getInsertDetails();
        assertThat(details.getPriority()).isEqualTo("DELAYED");
    }

    @Test
    @DisplayName("解析多表 UPDATE")
    void testMultiTableUpdate() {
        String sql = "UPDATE orders o\n" +
                "JOIN users u ON o.user_id = u.user_id\n" +
                "SET o.status = 2, o.updated_at = NOW()\n" +
                "WHERE o.status = 1 AND u.vip_level > 0";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isUpdate()).isTrue();
    }

    @Test
    @DisplayName("解析 ORDER BY ... LIMIT UPDATE")
    void testOrderByLimitUpdate() {
        String sql = "UPDATE tasks SET status = 'processing' WHERE status = 'pending' ORDER BY priority DESC, created_at ASC LIMIT 10";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isUpdate()).isTrue();
    }

    @Test
    @DisplayName("解析多表 DELETE")
    void testMultiTableDelete() {
        String sql = "DELETE o, oi FROM orders o\n" +
                "JOIN order_items oi ON o.order_id = oi.order_id\n" +
                "WHERE o.status = 4 AND o.created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isDelete()).isTrue();
    }

    @Test
    @DisplayName("解析 ORDER BY ... LIMIT DELETE")
    void testOrderByLimitDelete() {
        String sql = "DELETE FROM logs WHERE level = 'DEBUG' ORDER BY created_at ASC LIMIT 1000";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isDelete()).isTrue();
    }

    @Test
    @DisplayName("解析 REPLACE INTO")
    void testReplaceInto() {
        String sql = "REPLACE INTO users (id, username, email) VALUES (1, 'john', 'john@example.com')";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isInsert()).isTrue();
    }

    // ==================== 存储过程和函数测试 ====================

    @Test
    @DisplayName("解析 CREATE PROCEDURE")
    void testCreateProcedure() {
        String sql = "CREATE PROCEDURE GetUserOrders(IN userId BIGINT)\n" +
                "BEGIN\n" +
                "    SELECT * FROM orders WHERE user_id = userId;\n" +
                "END";

        SqlStatement stmt = parser.parse(sql);
        // CREATE PROCEDURE parsing test
    }

    @Test
    @DisplayName("解析 CREATE FUNCTION")
    void testCreateFunction() {
        String sql = "CREATE FUNCTION CalculateTax(amount DECIMAL(18,2))\n" +
                "RETURNS DECIMAL(18,2)\n" +
                "DETERMINISTIC\n" +
                "BEGIN\n" +
                "    RETURN amount * 0.13;\n" +
                "END";

        SqlStatement stmt = parser.parse(sql);
        // CREATE FUNCTION parsing test
    }

    @Test
    @DisplayName("解析 CREATE TRIGGER")
    void testCreateTrigger() {
        String sql = "CREATE TRIGGER update_order_timestamp\n" +
                "BEFORE UPDATE ON orders\n" +
                "FOR EACH ROW\n" +
                "BEGIN\n" +
                "    SET NEW.updated_at = NOW();\n" +
                "END";

        SqlStatement stmt = parser.parse(sql);
        // CREATE TRIGGER parsing test
    }

    @Test
    @DisplayName("解析 CREATE VIEW")
    void testCreateView() {
        String sql = "CREATE OR REPLACE VIEW v_active_users AS\n" +
                "SELECT user_id, username, email\n" +
                "FROM users\n" +
                "WHERE status = 1 AND deleted = 0\n" +
                "WITH CHECK OPTION";

        SqlStatement stmt = parser.parse(sql);
        // CREATE VIEW parsing test
    }

    @Test
    @DisplayName("解析 CREATE EVENT")
    void testCreateEvent() {
        String sql = "CREATE EVENT cleanup_old_logs\n" +
                "ON SCHEDULE EVERY 1 DAY\n" +
                "STARTS CURRENT_TIMESTAMP + INTERVAL 1 HOUR\n" +
                "DO\n" +
                "    DELETE FROM logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)";

        SqlStatement stmt = parser.parse(sql);
        // CREATE EVENT parsing test
    }

    // ==================== ALTER TABLE 测试 ====================

    @Test
    @DisplayName("解析 ALTER TABLE ADD COLUMN")
    void testAlterTableAddColumn() {
        String sql = "ALTER TABLE users ADD COLUMN phone VARCHAR(20) AFTER email";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD INDEX")
    void testAlterTableAddIndex() {
        String sql = "ALTER TABLE orders ADD INDEX idx_status_created (status, created_at) USING BTREE";

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
    @DisplayName("解析 ALTER TABLE MODIFY COLUMN")
    void testAlterTableModifyColumn() {
        String sql = "ALTER TABLE users MODIFY COLUMN email VARCHAR(200) NOT NULL";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE CHANGE COLUMN")
    void testAlterTableChangeColumn() {
        String sql = "ALTER TABLE users CHANGE COLUMN old_name new_name VARCHAR(100)";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD FOREIGN KEY")
    void testAlterTableAddForeignKey() {
        String sql = "ALTER TABLE order_items ADD CONSTRAINT fk_order\n" +
                "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE DROP FOREIGN KEY")
    void testAlterTableDropForeignKey() {
        String sql = "ALTER TABLE order_items DROP FOREIGN KEY fk_order";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE RENAME")
    void testAlterTableRename() {
        String sql = "ALTER TABLE old_table_name RENAME TO new_table_name";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE ADD PARTITION")
    void testAlterTableAddPartition() {
        String sql = "ALTER TABLE sales ADD PARTITION (PARTITION p2024 VALUES LESS THAN (2025))";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE DROP PARTITION")
    void testAlterTableDropPartition() {
        String sql = "ALTER TABLE sales DROP PARTITION p2021";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    @Test
    @DisplayName("解析 ALTER TABLE TRUNCATE PARTITION")
    void testAlterTableTruncatePartition() {
        String sql = "ALTER TABLE sales TRUNCATE PARTITION p2021";

        SqlStatement stmt = parser.parse(sql);
        // ALTER TABLE parsing test
    }

    // ==================== 其他复杂场景测试 ====================

    @Test
    @DisplayName("解析 LOCK TABLES")
    void testLockTables() {
        String sql = "LOCK TABLES orders WRITE, order_items READ";

        SqlStatement stmt = parser.parse(sql);
        // LOCK TABLES parsing test
    }

    @Test
    @DisplayName("解析 UNLOCK TABLES")
    void testUnlockTables() {
        String sql = "UNLOCK TABLES";

        SqlStatement stmt = parser.parse(sql);
        // UNLOCK TABLES parsing test
    }

    @Test
    @DisplayName("解析 FLUSH TABLES")
    void testFlushTables() {
        String sql = "FLUSH TABLES WITH READ LOCK";

        SqlStatement stmt = parser.parse(sql);
        // FLUSH parsing test
    }

    @Test
    @DisplayName("解析 SET 语句")
    void testSetStatement() {
        String sql = "SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_DATE'";

        SqlStatement stmt = parser.parse(sql);
        // SET parsing test
    }

    @Test
    @DisplayName("解析 PREPARE 语句")
    void testPrepareStatement() {
        String sql = "PREPARE stmt FROM 'SELECT * FROM users WHERE id = ?'";

        SqlStatement stmt = parser.parse(sql);
        // PREPARE parsing test
    }

    @Test
    @DisplayName("解析 EXECUTE 语句")
    void testExecuteStatement() {
        String sql = "EXECUTE stmt USING @user_id";

        SqlStatement stmt = parser.parse(sql);
        // EXECUTE parsing test
    }

    @Test
    @DisplayName("解析复杂 JSON 操作查询")
    void testJsonOperations() {
        String sql = "SELECT\n" +
                "    JSON_EXTRACT(data, '$.name') AS name,\n" +
                "    JSON_UNQUOTE(JSON_EXTRACT(data, '$.email')) AS email,\n" +
                "    JSON_CONTAINS(data, '{\"active\": true}') AS is_active,\n" +
                "    JSON_KEYS(data) AS keys\n" +
                "FROM json_table\n" +
                "WHERE JSON_VALID(data) = 1";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析全文搜索查询")
    void testFullTextSearch() {
        String sql = "SELECT * FROM articles\n" +
                "WHERE MATCH(title, content) AGAINST('MySQL database' IN NATURAL LANGUAGE MODE)\n" +
                "ORDER BY MATCH(title, content) AGAINST('MySQL database') DESC";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }

    @Test
    @DisplayName("解析空间数据查询")
    void testSpatialQuery() {
        String sql = "SELECT\n" +
                "    id,\n" +
                "    name,\n" +
                "    ST_Distance(location, POINT(116.4074, 39.9042)) AS distance\n" +
                "FROM places\n" +
                "WHERE ST_Contains(\n" +
                "    ST_GeomFromText('POLYGON((...))'),\n" +
                "    location\n" +
                ")\n" +
                "ORDER BY distance\n" +
                "LIMIT 10";

        SqlStatement stmt = parser.parse(sql);
        assertThat(stmt.isSelect()).isTrue();
    }
}
