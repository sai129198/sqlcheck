# SQL Parser 架构说明

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      SQL Parser                             │
├─────────────────────────────────────────────────────────────┤
│  Parser Layer                                               │
│  ├── SqlParser (基础解析器)                                  │
│  ├── PostgreSQLSqlParser                                    │
│  ├── MySQLSqlParser                                         │
│  ├── HiveSqlParser                                          │
│  ├── SparkSqlParser                                         │
│  ├── PrestoSqlParser                                        │
│  ├── ClickHouseSqlParser                                    │
│  └── StarRocksSqlParser                                     │
├─────────────────────────────────────────────────────────────┤
│  Model Layer                                                │
│  ├── 通用模型 (QueryBlock, TableRef, ColumnRef 等)           │
│  └── 方言特有模型 (postgresql/, mysql/, hive/ 等)            │
├─────────────────────────────────────────────────────────────┤
│  Test Layer                                                 │
│  └── 各解析器对应的测试类                                     │
└─────────────────────────────────────────────────────────────┘
```

## 核心类说明

### 解析器基类

**SqlParser**
- 位置: `src/main/java/com/example/sqlparser/parser/SqlParser.java`
- 职责: 提供基础 SQL 解析功能
- 方法: `parse(String sql)` - 解析 SQL 语句

### 方言解析器

所有方言解析器都继承自 `SqlParser`，并添加方言特有功能：

| 解析器 | 特有功能 |
|--------|----------|
| PostgreSQLSqlParser | CREATE TABLE 解析、ON CONFLICT、RETURNING、数组类型 |
| MySQLSqlParser | CREATE TABLE 解析、ON DUPLICATE KEY UPDATE、LIMIT 语法 |
| HiveSqlParser | Hive 特有语法支持 |
| SparkSqlParser | Spark SQL 特有语法支持 |
| PrestoSqlParser | Presto 特有语法支持 |
| ClickHouseSqlParser | ClickHouse 特有语法支持 |
| StarRocksSqlParser | StarRocks 特有语法支持 |

### 核心模型

**SqlStatement**
- 表示一个 SQL 语句
- 包含类型 (SELECT/INSERT/UPDATE/DELETE/CREATE/...)
- 包含各类语句的详细信息

**QueryBlock**
- 表示一个查询块
- 包含 SELECT、FROM、WHERE、GROUP BY、HAVING、ORDER BY、LIMIT 子句

**TableRef**
- 表示表引用
- 支持别名、子查询、JOIN 等

**ColumnRef**
- 表示列引用
- 支持表名限定、别名等

## 解析流程

```
SQL String
    │
    ▼
┌─────────────────┐
│   SqlParser     │
│   .parse()      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  1. 词法分析     │
│  2. 语法分析     │
│  3. 构建 AST     │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  SqlStatement   │
│  (包含详细结构)  │
└─────────────────┘
```

## 扩展机制

### 添加新的方言

1. 继承 `SqlParser` 类
2. 重写 `parse()` 方法添加方言特有功能
3. 添加方言特有的模型类
4. 创建对应的测试类

### 扩展现有方言

1. 在对应的模型类中添加新属性
2. 在解析器中添加解析逻辑
3. 添加对应的测试用例

## 设计原则

1. **开闭原则**: 对扩展开放，对修改封闭
2. **单一职责**: 每个解析器只负责一种方言
3. **复用**: 基础功能由父类提供，方言特有功能由子类实现
4. **可测试**: 每个解析器都有完整的测试覆盖

## 性能考虑

- 使用正则表达式进行快速匹配
- 避免在循环中创建不必要的对象
- 缓存常用的 Pattern 对象
