# SQL Parser

通用 SQL 解析器，支持 SELECT/INSERT/UPDATE/DELETE 语句解析和字段血缘分析。

## 功能特性

- **多语句类型支持**: SELECT, INSERT, UPDATE, DELETE
- **CTE (WITH 子句)**: 支持多层 CTE 定义
- **集合操作**: UNION, UNION ALL
- **JOIN 支持**: INNER JOIN, LEFT JOIN 等
- **字段血缘分析**: 追踪字段从源表到输出的完整链路
- **统一模型**: 所有语句类型使用一致的模型结构

## 项目结构

```
sql-parser/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/sqlparser/
│   │           ├── model/           # 数据模型
│   │           │   ├── SqlStatement.java      # 根对象
│   │           │   ├── SelectDetails.java     # SELECT 详情
│   │           │   ├── InsertDetails.java     # INSERT 详情
│   │           │   ├── UpdateDetails.java     # UPDATE 详情
│   │           │   ├── DeleteDetails.java     # DELETE 详情
│   │           │   ├── QueryBlock.java        # 查询块
│   │           │   ├── ColumnRef.java         # 字段引用
│   │           │   ├── TableRef.java          # 表引用
│   │           │   ├── LineageInfo.java       # 血缘信息
│   │           │   └── ...
│   │           └── parser/
│   │               └── SqlParser.java         # 解析器实现
│   └── test/
│       └── java/
│           └── com/example/sqlparser/
│               └── parser/
│                   ├── SqlParserTest.java         # 基础测试
│                   └── SqlParserAdvancedTest.java # 高级测试
```

## 核心模型

### SqlStatement（根对象）

```java
SqlStatement
├── type: SELECT/INSERT/UPDATE/DELETE
├── ctes: List<CteDefinition>           # WITH 子句
├── subQueries: List<SubQueryRef>       # 子查询
├── selectDetails: SelectDetails        # SELECT 时填充
├── insertDetails: InsertDetails        # INSERT 时填充
├── updateDetails: UpdateDetails        # UPDATE 时填充
└── deleteDetails: DeleteDetails        # DELETE 时填充
```

### 字段血缘

```java
ColumnRef
├── name: 字段名
├── alias: 别名
└── lineage: LineageInfo
    ├── expressionType: COLUMN/ADD/MULTIPLY/FUNCTION 等
    ├── sources: List<LineageSource>    # 血缘来源
    │   ├── type: TABLE_COLUMN/CTE_COLUMN/CONSTANT/EXPRESSION
    │   ├── tableColumn: TableColumnRef
    │   ├── constant: ConstantValue
    │   └── nestedExpression: LineageInfo  # 递归
    └── transforms: List<TransformFunction>  # 转换函数
```

## 使用示例

### 解析 SELECT

```java
SqlParser parser = new SqlParser();
SqlStatement stmt = parser.parse("SELECT id, name FROM users WHERE age > 18");

// 获取表
List<TableRef> tables = stmt.getAllTables();
// [TableRef(name="users")]

// 获取字段血缘
List<ColumnLineage> lineage = stmt.getAllColumnLineage();
// [ColumnLineage(targetColumn="id"), ColumnLineage(targetColumn="name")]
```

### 解析 INSERT ... SELECT

```java
SqlStatement stmt = parser.parse(
    "INSERT INTO target (id, name) SELECT user_id, user_name FROM source"
);

InsertDetails details = stmt.getInsertDetails();
// target 表: details.getTargetTable()
// 源查询: details.getSelectQuery()
// 字段映射: details.getColumnMappings()
```

### 解析带表达式的 SELECT

```java
SqlStatement stmt = parser.parse(
    "SELECT price * quantity AS total FROM orders"
);

ColumnRef totalCol = stmt.getSelectDetails()
    .getQueryBlock()
    .getSelect()
    .getColumns()
    .get(0);

// 表达式类型: MULTIPLY
ExpressionType type = totalCol.getLineage().getExpressionType();

// 血缘来源: price, quantity
List<LineageSource> sources = totalCol.getLineage().getSources();
```

### 解析 CTE

```java
SqlStatement stmt = parser.parse(
    "WITH cte AS (SELECT id FROM users) SELECT * FROM cte"
);

List<CteDefinition> ctes = stmt.getCtes();
// [CteDefinition(name="cte", query=QueryBlock)]
```

## 环境要求

- Java 8 或更高版本
- Maven 3.6+

## 构建和测试

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package
```

### 指定 Java 版本编译

如果本地有多个 Java 版本，可以指定使用 Java 8：

```bash
export JAVA_HOME=/path/to/java8
mvn clean package
```

## 测试覆盖

- 基础 SELECT 解析
- 带 WHERE/GROUP BY/ORDER BY/LIMIT 的 SELECT
- JOIN 解析
- UNION/UNION ALL
- INSERT VALUES/SELECT
- UPDATE 单表/多表
- DELETE
- CTE (WITH 子句)
- 字段血缘分析
- 复杂表达式解析

## 注意事项

当前实现为简化版解析器，基于正则表达式和关键字匹配。生产环境建议使用 ANTLR4 生成完整的 SQL 语法解析器。

支持的 SQL 方言：
- MySQL（主要测试）
- 部分 PostgreSQL 语法

### Java 版本兼容性

- 源码兼容 Java 8+
- 使用 Maven 编译时自动适配 Java 8 字节码
- 不使用 Java 9+ 的新特性（如 `var`、`List.of()`、`Stream.toList()` 等）

## 扩展计划

- [ ] 集成 ANTLR4 完整语法
- [ ] 支持更多 SQL 方言（Oracle、SQL Server）
- [ ] 子查询血缘追踪优化
- [ ] 数据血缘可视化输出
- [ ] 血缘影响分析（反向追踪）
