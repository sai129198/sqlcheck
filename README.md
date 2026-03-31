# SQL Parser

一个支持多数据库方言的 SQL 解析器，基于 Java 开发。

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

```java
// 基础 SQL 解析
SqlParser parser = new SqlParser();
SqlStatement stmt = parser.parse("SELECT * FROM users WHERE id = 1");

// PostgreSQL 解析
PostgreSQLSqlParser pgParser = new PostgreSQLSqlParser();
PostgreSQLCreateTable table = pgParser.parseCreateTable(
    "CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100))"
);

// MySQL 解析
MySQLSqlParser mysqlParser = new MySQLSqlParser();
MySQLCreateTable mysqlTable = mysqlParser.parseCreateTable(
    "CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY)"
);
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
│   │   │   └── ...             # 通用模型
│   │   └── parser/             # SQL 解析器
│   │       ├── SqlParser.java
│   │       ├── PostgreSQLSqlParser.java
│   │       ├── MySQLSqlParser.java
│   │       └── ...
│   └── test/java/com/example/sqlparser/
│       └── parser/             # 测试类
├── docs/                       # 文档
│   ├── postgresql/             # PostgreSQL 文档
│   └── guides/                 # 使用指南
└── README.md
```

## 文档

- [PostgreSQL 语法指南](docs/postgresql/postgresql_guide.md) - 完整的 PostgreSQL SQL 语法参考
- [PostgreSQL 测试 SQL](docs/postgresql/postgresql_test.sql) - 可执行的测试 SQL 文件

## 特性

### 通用 SQL 支持
- ✅ SELECT / INSERT / UPDATE / DELETE
- ✅ JOIN (INNER, LEFT, RIGHT, FULL)
- ✅ 子查询和 CTE
- ✅ 聚合函数和窗口函数
- ✅ 集合操作 (UNION, INTERSECT, EXCEPT)

### PostgreSQL 特有功能
- ✅ CREATE TABLE (TEMPORARY/UNLOGGED/IF NOT EXISTS)
- ✅ SERIAL/BIGSERIAL 类型
- ✅ 数组类型
- ✅ 分区表 (RANGE/LIST/HASH)
- ✅ ON CONFLICT (UPSERT)
- ✅ RETURNING 子句
- ✅ DISTINCT ON
- ✅ FOR UPDATE 变体

### MySQL 特有功能
- ✅ AUTO_INCREMENT
- ✅ INSERT IGNORE / ON DUPLICATE KEY UPDATE
- ✅ LIMIT offset,count
- ✅ SQL_CALC_FOUND_ROWS

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

[Apache License 2.0](LICENSE)
