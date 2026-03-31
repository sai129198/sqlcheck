# PostgreSQL 文档

本目录包含 PostgreSQL 相关的学习资料和测试文件。

## 文件说明

| 文件 | 说明 |
|------|------|
| [postgresql_guide.md](postgresql_guide.md) | 完整的 PostgreSQL SQL 语法指南（21KB） |
| [postgresql_test.sql](postgresql_test.sql) | 可执行的测试 SQL 文件（19KB） |

## 快速开始

### 安装 PostgreSQL

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

**macOS:**
```bash
brew install postgresql
brew services start postgresql
```

**Docker:**
```bash
docker run --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres
```

### 运行测试 SQL

```bash
# 创建测试数据库
createdb test_db

# 执行测试文件
psql -d test_db -f postgresql_test.sql
```

## 内容概览

### 语法指南包含
- 数据类型（数值、字符、日期、JSONB、数组等）
- DDL（CREATE TABLE、ALTER TABLE、索引等）
- DML（INSERT、UPDATE、DELETE、UPSERT）
- DQL（SELECT、JOIN、子查询、窗口函数）
- 高级特性（分区表、CTE、递归查询、全文搜索）
- 性能优化

### 测试文件包含
- 完整的表结构定义
- 示例数据
- 各种 SQL 语句的测试用例
- 性能分析查询

## 常用命令

```sql
-- 列出所有数据库
\l

-- 切换数据库
\c database_name

-- 列出所有表
\dt

-- 查看表结构
\d table_name

-- 退出 psql
\q
```

## 参考资料

- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [PostgreSQL 中文社区](http://www.postgres.cn/)
