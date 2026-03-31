# SQL Parser 开发指南

## 如何添加新的 SQL 方言支持

如果你想为项目添加新的数据库方言支持，可以按照以下步骤进行：

### 1. 创建模型类

在 `src/main/java/com/example/sqlparser/model/<dialect>/` 目录下创建模型类：

```java
// <Dialect>CreateTable.java
package com.example.sqlparser.model.<dialect>;

import lombok.Data;
import java.util.List;

@Data
public class <Dialect>CreateTable {
    private String tableName;
    private List<<Dialect>ColumnDef> columns;
    // 方言特有属性
}

// <Dialect>ColumnDef.java
@Data
public class <Dialect>ColumnDef {
    private String name;
    private String dataType;
    // 方言特有属性
}

// <Dialect>SelectDetails.java
@Data
public class <Dialect>SelectDetails extends SelectDetails {
    // 方言特有 SELECT 属性
}
```

### 2. 创建解析器

在 `src/main/java/com/example/sqlparser/parser/` 目录下创建解析器：

```java
package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.<dialect>.*;

public class <Dialect>SqlParser extends SqlParser {
    
    public <Dialect>CreateTable parseCreateTable(String sql) {
        <Dialect>CreateTable table = new <Dialect>CreateTable();
        // 解析逻辑
        return table;
    }
    
    @Override
    public SqlStatement parse(String sql) {
        SqlStatement stmt = super.parse(sql);
        // 扩展方言特有功能
        return stmt;
    }
}
```

### 3. 创建测试类

在 `src/test/java/com/example/sqlparser/parser/` 目录下创建测试：

```java
package com.example.sqlparser.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class <Dialect>SqlParserTest {
    private <Dialect>SqlParser parser = new <Dialect>SqlParser();
    
    @Test
    void testBasicCreateTable() {
        String sql = "CREATE TABLE users (id INT PRIMARY KEY)";
        <Dialect>CreateTable table = parser.parseCreateTable(sql);
        assertThat(table.getTableName()).isEqualTo("users");
    }
}
```

### 4. 参考实现

可以参考以下已有的实现：

- **PostgreSQL**: `PostgreSQLSqlParser.java` - 最完整的实现，包含分区、UPSERT、RETURNING 等
- **MySQL**: `MySQLSqlParser.java` - 包含 ON DUPLICATE KEY UPDATE、LIMIT 语法等
- **Hive**: `HiveSqlParser.java` - 大数据场景

### 5. 测试要求

- 至少包含 20 个测试用例
- 覆盖 CREATE TABLE 的各种场景
- 覆盖方言特有的 SQL 语法
- 所有测试必须通过

### 6. 提交代码

```bash
git add -A
git commit -m "Add <Dialect> SQL parser support

- Add <Dialect>SqlParser
- Add <Dialect>-specific models
- Support features: ...
- Add comprehensive test suite"
git push origin main
```

## 代码规范

1. **命名规范**: 类名使用 PascalCase，方法名和变量名使用 camelCase
2. **注释**: 公共方法必须添加 JavaDoc 注释
3. **测试**: 使用 JUnit 5 和 AssertJ 进行测试
4. **日志**: 使用 SLF4J 进行日志记录

## 常见问题

### Q: 如何处理复杂的正则表达式？

A: 参考 `PostgreSQLSqlParser.parseColumns()` 方法，使用手动解析代替复杂正则。

### Q: 如何支持嵌套结构？

A: 使用栈或递归方法处理括号嵌套，参考 `smartSplit()` 方法。

### Q: 如何处理关键字大小写？

A: 统一将 SQL 转换为大写进行比较，保留原始字符串用于提取值。
