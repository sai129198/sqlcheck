# SQL Parser

一个支持多数据库方言的 SQL 解析器，基于 Java 开发。

## 用途与应用场景

SQL Parser 可以将 SQL 语句解析为结构化的 Java 对象，适用于以下场景：

### 1. SQL 血缘分析 (Lineage Analysis)
- 分析 SQL 中的表依赖关系
- 追踪数据从源表到目标表的流动
- 构建数据血缘图谱
- 适用于数据治理、影响分析

### 2. SQL 审核与规范检查
- 检查 SQL 是否符合编码规范
- 识别潜在的性能问题（如全表扫描）
- 验证 SQL 语法正确性
- 自动化 SQL 代码审查

### 3. SQL 转换与迁移
- 在不同数据库方言之间转换 SQL
- 将 MySQL SQL 转换为 PostgreSQL SQL
- 生成兼容多种数据库的 SQL

### 4. 动态 SQL 构建
- 程序化地构建复杂 SQL
- 动态添加 WHERE 条件
- 自动生成分页查询

### 5. 数据血缘追踪
```java
// 示例：获取 SQL 中引用的所有表
SqlStatement stmt = parser.parse(
    "SELECT u.name, o.amount FROM users u JOIN orders o ON u.id = o.user_id"
);
List<TableRef> tables = stmt.getSelectDetails().getQueryBlock().getFromClause().getTables();
// 结果: [users, orders]
```

### 6. 敏感字段检测
- 识别 SQL 中是否包含敏感字段（如密码、身份证号）
- 检测数据脱敏情况
- 数据安全审计

## 支持的 SQL 方言

| 方言 | 解析器 | 状态 |
|------|--------|------|
| 通用 SQL | `SqlParser` | ✅ 支持 |
| MySQL | `MySQLSqlParser` | ✅ 支持 |
| PostgreSQL | `PostgreSQLSqlParser` | ✅ 支持 |
| Hive | `HiveSqlParser` | ✅ 支持 |
| Spark SQL | `SparkSqlParser` | ✅ 支持 |
| Presto/Trino | `PrestoSqlParser` | ✅ 支持 |
| ClickHouse | `ClickHouseSqlParser` | ✅ 支持 |
| StarRocks | `StarRocksSqlParser` | ✅ 支持 |
| **Impala** | `ImpalaSqlParser` | ✅ 支持 |

## 快速开始

### 构建项目

```bash
mvn clean compile
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定解析器的测试
mvn test -Dtest=PostgreSQLSqlParserTest
```

### 使用示例

#### 基础 SQL 解析
```java
SqlParser parser = new SqlParser();
SqlStatement stmt = parser.parse("SELECT * FROM users WHERE id = 1");
System.out.println(stmt.getType()); // SELECT
```

#### 获取 SQL 中的表
```java
SqlStatement stmt = parser.parse(
    "SELECT * FROM users u JOIN orders o ON u.id = o.user_id"
);
List<TableRef> tables = stmt.getSelectDetails()
    .getQueryBlock()
    .getFromClause()
    .getTables();
// tables: [users (alias: u), orders (alias: o)]
```

#### 解析 CREATE TABLE
```java
// PostgreSQL
PostgreSQLSqlParser pgParser = new PostgreSQLSqlParser();
PostgreSQLCreateTable table = pgParser.parseCreateTable(
    "CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100))"
);
System.out.println(table.getTableName()); // users
System.out.println(table.getColumns().size()); // 2

// MySQL
MySQLSqlParser mysqlParser = new MySQLSqlParser();
MySQLCreateTable mysqlTable = mysqlParser.parseCreateTable(
    "CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY)"
);
System.out.println(mysqlTable.getEngine()); // InnoDB
```

#### 解析 INSERT ON CONFLICT (PostgreSQL UPSERT)
```java
PostgreSQLSqlParser parser = new PostgreSQLSqlParser();
SqlStatement stmt = parser.parse(
    "INSERT INTO users (id, name) VALUES (1, 'John') " +
    "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name"
);
PostgreSQLInsertDetails details = (PostgreSQLInsertDetails) stmt.getInsertDetails();
System.out.println(details.isOnConflict()); // true
```

#### 解析 Impala CREATE TABLE
```java
ImpalaSqlParser impalaParser = new ImpalaSqlParser();

// 基础 CREATE TABLE
ImpalaCreateTable table = impalaParser.parseCreateTable(
    "CREATE TABLE users (id BIGINT, name STRING) STORED AS PARQUET"
);
System.out.println(table.getTableName()); // users
System.out.println(table.getStoredAs()); // PARQUET

// Kudu 表
ImpalaCreateTable kuduTable = impalaParser.parseCreateTable(
    "CREATE TABLE events (id BIGINT PRIMARY KEY, data STRING) " +
    "DISTRIBUTE BY HASH (id) INTO 16 BUCKETS STORED AS KUDU"
);
System.out.println(kuduTable.isKuduTable()); // true
System.out.println(kuduTable.getPrimaryKey()); // [id]

// 缓存表
ImpalaCreateTable cachedTable = impalaParser.parseCreateTable(
    "CREATE TABLE hot_data (id INT) CACHED IN 'pool1' WITH REPLICATION = 2"
);
System.out.println(cachedTable.isCached()); // true
System.out.println(cachedTable.getCacheReplication()); // 2
```

#### 解析 Impala COMPUTE STATS
```java
ImpalaSqlParser parser = new ImpalaSqlParser();
SqlStatement stmt = parser.parse("COMPUTE STATS mydb.users");
ImpalaMetadataStmt metadata = stmt.getMetadataStmt();
System.out.println(metadata.getStmtType()); // COMPUTE_STATS
System.out.println(metadata.getDatabase()); // mydb
System.out.println(metadata.getTableName()); // users
```

#### 解析 Impala SELECT with hints
```java
ImpalaSqlParser parser = new ImpalaSqlParser();
SqlStatement stmt = parser.parse(
    "SELECT STRAIGHT_JOIN * FROM users u JOIN orders o ON u.id = o.user_id [SHUFFLE]"
);
ImpalaSelectDetails details = (ImpalaSelectDetails) stmt.getSelectDetails();
System.out.println(details.isStraightJoin()); // true
System.out.println(details.isShuffle()); // true
```

## 项目结构

```
sql-parser/
├── src/
│   ├── main/java/com/example/sqlparser/
│   │   ├── model/              # 数据模型
│   │   │   ├── postgresql/     # PostgreSQL 模型
│   │   │   ├── mysql/          # MySQL 模型
│   │   │   ├── hive/           # Hive 模型
│   │   │   ├── spark/          # Spark SQL 模型
│   │   │   ├── presto/         # Presto 模型
│   │   │   ├── clickhouse/     # ClickHouse 模型
│   │   │   ├── starrocks/      # StarRocks 模型
│   │   │   ├── impala/         # Impala 模型
│   │   │   └── ...             # 通用模型
│   │   └── parser/             # SQL 解析器
│   │       ├── SqlParser.java
│   │       ├── PostgreSQLSqlParser.java
│   │       ├── MySQLSqlParser.java
│   │       ├── ImpalaSqlParser.java
│   │       └── ...
│   └── test/java/com/example/sqlparser/
│       └── parser/             # 测试类
├── docs/                       # 文档
│   ├── postgresql/             # PostgreSQL 文档
│   └── guides/                 # 使用指南
└── README.md
```

## 文档

- [使用指南](docs/guides/usage_guide.md) - 详细的使用示例和 API 说明
- [开发指南](docs/guides/development_guide.md) - 如何添加新的 SQL 方言支持
- [架构说明](docs/guides/architecture.md) - 项目架构和设计说明
- [Maven 发布指南](docs/guides/maven_deployment.md) - 如何发布到 Maven 仓库
- [PostgreSQL 语法指南](docs/postgresql/postgresql_guide.md) - 完整的 PostgreSQL SQL 语法参考
- [PostgreSQL 测试 SQL](docs/postgresql/postgresql_test.sql) - 可执行的测试 SQL 文件

## 核心功能

### 1. SQL 解析能力

#### 通用 SQL 支持
- ✅ SELECT / INSERT / UPDATE / DELETE / CREATE / ALTER / DROP
- ✅ JOIN (INNER, LEFT, RIGHT, FULL, CROSS)
- ✅ 子查询和 CTE (WITH 子句)
- ✅ 聚合函数和窗口函数
- ✅ 集合操作 (UNION, INTERSECT, EXCEPT)

#### PostgreSQL 特有功能
- ✅ CREATE TABLE (TEMPORARY/UNLOGGED/IF NOT EXISTS)
- ✅ SERIAL/BIGSERIAL 类型、数组类型
- ✅ 分区表 (RANGE/LIST/HASH)
- ✅ ON CONFLICT (UPSERT)
- ✅ RETURNING 子句
- ✅ DISTINCT ON、FOR UPDATE 变体

#### MySQL 特有功能
- ✅ AUTO_INCREMENT
- ✅ INSERT IGNORE / ON DUPLICATE KEY UPDATE
- ✅ LIMIT offset,count
- ✅ SQL_CALC_FOUND_ROWS

#### Impala 特有功能
- ✅ CREATE TABLE (EXTERNAL/IF NOT EXISTS)
- ✅ STORED AS (PARQUET/AVRO/TEXTFILE/RCFILE/SEQUENCEFILE/ORC/KUDU)
- ✅ PARTITIONED BY / DISTRIBUTE BY / SORT BY
- ✅ Kudu 表支持 (PRIMARY KEY)
- ✅ CACHED / UNCACHED
- ✅ COMPUTE STATS / DROP STATS
- ✅ REFRESH / INVALIDATE METADATA
- ✅ SELECT [STRAIGHT_JOIN] [SHUFFLE | NOSHUFFLE]
- ✅ INSERT [SHUFFLE | NOSHUFFLE]
- ✅ SHOW TABLES/DATABASES/PARTITIONS/COLUMN STATS
- ✅ DESCRIBE FORMATTED/EXTENDED
- ✅ EXPLAIN LEVEL (MINIMAL/STANDARD/EXTENDED/VERBOSE)

### 2. 元数据提取
- 提取 SQL 中引用的所有表和列
- 解析表别名和列别名
- 识别 JOIN 条件和 WHERE 条件
- 提取子查询结构

### 3. SQL 结构分析
- 识别 SQL 语句类型
- 解析查询块结构
- 分析复杂的嵌套查询
- 支持多语句解析

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

[Apache License 2.0](LICENSE)
