# PostgreSQL 测试指南

## 文件说明

本目录包含 PostgreSQL 的完整学习资料：

| 文件 | 说明 |
|------|------|
| `postgresql_guide.md` | 完整的 SQL 语法指南 |
| `postgresql_test.sql` | 可执行的测试 SQL 文件 |
| `README.md` | 本文件 |

---

## 快速开始

### 1. 安装 PostgreSQL

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

### 2. 创建测试数据库

```bash
# 切换到 postgres 用户
sudo -u postgres psql

# 在 psql 中执行
CREATE DATABASE test_db;
CREATE USER test_user WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE test_db TO test_user;
\q
```

### 3. 运行测试文件

```bash
# 方法1: 使用 psql 命令行
psql -U test_user -d test_db -f postgresql_test.sql

# 方法2: 进入 psql 后执行
psql -U test_user -d test_db
\i postgresql_test.sql
```

---

## 测试内容概览

### 基础测试
- ✅ 数据类型定义
- ✅ 表创建 (含约束、默认值)
- ✅ 索引创建 (B-tree、GIN、部分索引等)
- ✅ 插入、更新、删除操作

### 高级特性测试
- ✅ 分区表
- ✅ JSONB 操作
- ✅ 数组类型
- ✅ 全文搜索
- ✅ 窗口函数
- ✅ CTE (公用表表达式)
- ✅ 递归查询

### 实用功能测试
- ✅ 视图和物化视图
- ✅ 存储过程和函数
- ✅ 触发器
- ✅ 事务控制
- ✅ UPSERT (INSERT ON CONFLICT)

---

## 常用命令速查

### 连接数据库
```bash
psql -U username -d database_name -h hostname -p port
```

### psql 常用命令
```sql
\l              -- 列出所有数据库
\c database     -- 切换数据库
\dt             -- 列出所有表
\d table_name   -- 查看表结构
\di             -- 列出所有索引
\dv             -- 列出所有视图
\df             -- 列出所有函数
\du             -- 列出所有用户
\timing on      -- 开启执行时间显示
\x              -- 切换扩展显示模式
\q              -- 退出 psql
```

### 数据库管理
```sql
-- 创建数据库
CREATE DATABASE dbname;

-- 删除数据库
DROP DATABASE dbname;

-- 备份数据库
pg_dump -U username -d database_name > backup.sql

-- 恢复数据库
psql -U username -d database_name < backup.sql

-- 导出特定表
pg_dump -U username -d database_name -t table_name > table_backup.sql
```

---

## 性能优化建议

### 1. 索引优化
```sql
-- 查看查询执行计划
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM users WHERE email = 'test@example.com';

-- 创建合适的索引
CREATE INDEX CONCURRENTLY idx_users_email ON users(email);

-- 删除未使用的索引
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
```

### 2. 查询优化
```sql
-- 使用 LIMIT 分页
SELECT * FROM posts ORDER BY created_at DESC LIMIT 10 OFFSET 20;

-- 使用游标 (大数据集)
DECLARE cursor_posts CURSOR FOR SELECT * FROM posts;
FETCH 10 FROM cursor_posts;
```

### 3. 维护操作
```sql
-- 分析表
ANALYZE users;

-- 清理表
VACUUM ANALYZE posts;

-- 重建索引
REINDEX INDEX idx_users_email;
```

---

## 学习路径建议

### 初学者
1. 阅读 `postgresql_guide.md` 的基础部分
2. 运行测试文件中的基础查询
3. 练习 CRUD 操作

### 进阶学习
1. 深入学习索引类型和使用场景
2. 掌握 JOIN 和子查询
3. 学习窗口函数和 CTE

### 高级主题
1. 分区表设计和优化
2. JSONB 高级操作
3. 全文搜索配置
4. 性能调优和监控

---

## 参考资料

- [PostgreSQL 官方文档](https://www.postgresql.org/docs/)
- [PostgreSQL 中文社区](http://www.postgres.cn/)
- [pgAdmin](https://www.pgadmin.org/) - 图形化管理工具
- [DBeaver](https://dbeaver.io/) - 通用数据库工具

---

## 问题反馈

如有问题或建议，欢迎提出！
