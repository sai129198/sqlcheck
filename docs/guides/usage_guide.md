# SQL Parser 使用指南

## 目录

1. [快速开始](#快速开始)
2. [解析器选择](#解析器选择)
3. [使用示例](#使用示例)
4. [API 参考](#api-参考)

---

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>sql-parser</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 基础使用

```java
import com.example.sqlparser.parser.SqlParser;
import com.example.sqlparser.model.SqlStatement;

public class Example {
    public static void main(String[] args) {
        SqlParser parser = new SqlParser();
        SqlStatement stmt = parser.parse("SELECT * FROM users WHERE id = 1");
        
        System.out.println("Statement type: " + stmt.getType());
        System.out.println("Query block: " + stmt.getSelectDetails().getQueryBlock());
    }
}
```

---

## 解析器选择

根据你的数据库类型选择合适的解析器：

| 数据库 | 解析器类 | 说明 |
|--------|----------|------|
| PostgreSQL | `PostgreSQLSqlParser` | 支持 ON CONFLICT、RETURNING、数组类型等 |
| MySQL | `MySQLSqlParser` | 支持 ON DUPLICATE KEY UPDATE、LIMIT 语法等 |
| Hive | `HiveSqlParser` | 支持 Hive 特有语法 |
| Spark SQL | `SparkSqlParser` | 支持 Spark SQL 语法 |
| Presto/Trino | `PrestoSqlParser` | 支持 Presto 特有语法 |
| ClickHouse | `ClickHouseSqlParser` | 支持 ClickHouse 特有语法 |
| StarRocks | `StarRocksSqlParser` | 支持 StarRocks 特有语法 |
| 通用 | `SqlParser` | 标准 SQL 解析 |

---

## 使用示例

### PostgreSQL 示例

```java
import com.example.sqlparser.parser.PostgreSQLSqlParser;
import com.example.sqlparser.model.postgresql.*;

PostgreSQLSqlParser parser = new PostgreSQLSqlParser();

// 解析 CREATE TABLE
String createSql = "CREATE TABLE users (" +
    "id SERIAL PRIMARY KEY," +
    "name VARCHAR(100) NOT NULL," +
    "tags TEXT[]" +
    ") PARTITION BY RANGE (created_at)";

PostgreSQLCreateTable table = parser.parseCreateTable(createSql);
System.out.println("Table: " + table.getTableName());
System.out.println("Columns: " + table.getColumns().size());
System.out.println("Partition: " + table.getPartitionInfo().getPartitionType());

// 解析 SELECT
String selectSql = "SELECT * FROM users WHERE id = 1 FOR UPDATE SKIP LOCKED";
SqlStatement stmt = parser.parse(selectSql);
PostgreSQLSelectDetails details = (PostgreSQLSelectDetails) stmt.getSelectDetails();
System.out.println("For Update: " + details.isForUpdate());
System.out.println("Skip Locked: " + details.isForUpdateSkipLocked());

// 解析 INSERT ON CONFLICT
String upsertSql = "INSERT INTO users (id, name) VALUES (1, 'John') " +
    "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name " +
    "RETURNING id, name";
SqlStatement upsertStmt = parser.parse(upsertSql);
PostgreSQLInsertDetails insertDetails = (PostgreSQLInsertDetails) upsertStmt.getInsertDetails();
System.out.println("On Conflict: " + insertDetails.isOnConflict());
System.out.println("Returning: " + insertDetails.isReturning());
```

### MySQL 示例

```java
import com.example.sqlparser.parser.MySQLSqlParser;
import com.example.sqlparser.model.mysql.*;

MySQLSqlParser parser = new MySQLSqlParser();

// 解析 CREATE TABLE
String createSql = "CREATE TABLE users (" +
    "id INT AUTO_INCREMENT PRIMARY KEY," +
    "name VARCHAR(100)" +
    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

MySQLCreateTable table = parser.parseCreateTable(createSql);
System.out.println("Engine: " + table.getEngine());
System.out.println("Charset: " + table.getCharset());

// 解析 INSERT ON DUPLICATE KEY UPDATE
String upsertSql = "INSERT INTO users (id, name) VALUES (1, 'John') " +
    "ON DUPLICATE KEY UPDATE name = 'Jane'";
SqlStatement stmt = parser.parse(upsertSql);
MySQLInsertDetails details = (MySQLInsertDetails) stmt.getInsertDetails();
System.out.println("On Duplicate Updates: " + details.getOnDuplicateKeyUpdates().size());
```

---

## API 参考

### SqlParser

基础 SQL 解析器，支持标准 SQL 语法。

```java
SqlStatement parse(String sql)
```

### PostgreSQLSqlParser

PostgreSQL 特有方法：

```java
PostgreSQLCreateTable parseCreateTable(String sql)
```

### MySQLSqlParser

MySQL 特有方法：

```java
MySQLCreateTable parseCreateTable(String sql)
```

---

## 更多示例

查看测试文件获取更多使用示例：

- `PostgreSQLSqlParserTest.java` - PostgreSQL 解析器测试
- `MySQLSqlParserTest.java` - MySQL 解析器测试
- `SqlParserTest.java` - 基础解析器测试
