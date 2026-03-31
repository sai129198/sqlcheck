# PostgreSQL SQL 语法指南与测试

## 目录
1. [数据类型](#数据类型)
2. [DDL - 数据定义语言](#ddl---数据定义语言)
3. [DML - 数据操作语言](#dml---数据操作语言)
4. [DQL - 数据查询语言](#dql---数据查询语言)
5. [高级特性](#高级特性)
6. [性能优化](#性能优化)

---

## 数据类型

### 数值类型
```sql
-- 整数类型
SMALLINT    -- 2字节, -32768 到 32767
INTEGER     -- 4字节, -2147483648 到 2147483647
BIGINT      -- 8字节, 大范围整数
SERIAL      -- 自增整数 (4字节)
BIGSERIAL   -- 自增大整数 (8字节)

-- 浮点类型
REAL        -- 4字节, 单精度
DOUBLE PRECISION  -- 8字节, 双精度
NUMERIC(p,s)      -- 精确小数, p=总位数, s=小数位
DECIMAL(p,s)      -- NUMERIC 的别名
```

### 字符类型
```sql
CHAR(n)         -- 固定长度, 不足补空格
VARCHAR(n)      -- 变长, 有长度限制
TEXT            -- 变长, 无限制
```

### 日期时间类型
```sql
DATE              -- 日期 (年-月-日)
TIME              -- 时间 (时:分:秒)
TIMESTAMP         -- 日期+时间
TIMESTAMPTZ       -- 带时区的日期时间
INTERVAL          -- 时间间隔
```

### 其他常用类型
```sql
BOOLEAN           -- true/false
BYTEA             -- 二进制数据
UUID              -- UUID 值
JSON              -- JSON 数据
JSONB             -- 二进制 JSON (推荐, 支持索引)
ARRAY             -- 数组类型
ENUM              -- 枚举类型
INET/CIDR         -- IP 地址
```

---

## DDL - 数据定义语言

### 创建数据库
```sql
-- 创建数据库
CREATE DATABASE mydb
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- 删除数据库
DROP DATABASE IF EXISTS mydb;
```

### 创建表
```sql
-- 基本表创建
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    age INTEGER CHECK (age >= 0 AND age <= 150),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_data JSONB,
    tags TEXT[]
);

-- 带外键的表
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(20) DEFAULT 'draft' 
        CHECK (status IN ('draft', 'published', 'archived')),
    view_count INTEGER DEFAULT 0,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建枚举类型
CREATE TYPE user_role AS ENUM ('admin', 'moderator', 'user', 'guest');

-- 使用枚举类型
CREATE TABLE user_roles (
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    role user_role NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role)
);
```

### 修改表结构
```sql
-- 添加列
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN address TEXT DEFAULT '';

-- 修改列
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(150);
ALTER TABLE users ALTER COLUMN age SET NOT NULL;
ALTER TABLE users ALTER COLUMN age DROP NOT NULL;
ALTER TABLE users ALTER COLUMN is_active SET DEFAULT false;

-- 删除列
ALTER TABLE users DROP COLUMN IF EXISTS phone;

-- 重命名列
ALTER TABLE users RENAME COLUMN full_name TO display_name;

-- 重命名表
ALTER TABLE users RENAME TO app_users;
ALTER TABLE app_users RENAME TO users;

-- 添加约束
ALTER TABLE posts ADD CONSTRAINT fk_posts_user 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE users ADD CONSTRAINT chk_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- 删除约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_email_format;

-- 添加索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_status ON posts(status) WHERE status = 'published';

-- 复合索引
CREATE INDEX idx_posts_user_status ON posts(user_id, status);

-- GIN 索引 (用于 JSONB 和数组)
CREATE INDEX idx_users_profile ON users USING GIN(profile_data);
CREATE INDEX idx_users_tags ON users USING GIN(tags);

-- 唯一索引
CREATE UNIQUE INDEX idx_users_username_lower ON users(LOWER(username));

-- 删除索引
DROP INDEX IF EXISTS idx_users_email;
```

### 分区表
```sql
-- 创建分区表 (按范围分区)
CREATE TABLE events (
    id BIGSERIAL,
    event_type VARCHAR(50),
    event_data JSONB,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE events_2024_q1 PARTITION OF events
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE events_2024_q2 PARTITION OF events
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE events_2024_q3 PARTITION OF events
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE events_2024_q4 PARTITION OF events
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');
```

---

## DML - 数据操作语言

### INSERT
```sql
-- 单行插入
INSERT INTO users (username, email, password_hash, full_name, age)
VALUES ('john_doe', 'john@example.com', 'hash123', 'John Doe', 30);

-- 多行插入
INSERT INTO users (username, email, password_hash, full_name, age)
VALUES 
    ('jane_smith', 'jane@example.com', 'hash456', 'Jane Smith', 25),
    ('bob_wilson', 'bob@example.com', 'hash789', 'Bob Wilson', 35),
    ('alice_brown', 'alice@example.com', 'hash101', 'Alice Brown', 28);

-- 插入并返回数据
INSERT INTO users (username, email, password_hash)
VALUES ('new_user', 'new@example.com', 'hash999')
RETURNING id, username, created_at;

-- 从其他表插入
INSERT INTO users_archive (id, username, email, archived_at)
SELECT id, username, email, CURRENT_TIMESTAMP
FROM users 
WHERE is_active = false;

-- 插入或更新 (UPSERT)
INSERT INTO users (id, username, email, password_hash)
VALUES (1, 'updated_user', 'updated@example.com', 'newhash')
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    updated_at = CURRENT_TIMESTAMP;

-- 插入时忽略冲突
INSERT INTO users (username, email, password_hash)
VALUES ('john_doe', 'john@example.com', 'hash123')
ON CONFLICT (username) DO NOTHING;
```

### UPDATE
```sql
-- 基本更新
UPDATE users 
SET is_active = false 
WHERE id = 1;

-- 多列更新
UPDATE users 
SET 
    full_name = 'John Updated',
    age = 31,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 1;

-- 使用子查询更新
UPDATE posts 
SET view_count = view_count + 1
WHERE id IN (
    SELECT post_id FROM post_views 
    WHERE viewed_at > CURRENT_DATE - INTERVAL '7 days'
);

-- 关联更新
UPDATE posts p
SET view_count = v.total_views
FROM (
    SELECT post_id, COUNT(*) as total_views 
    FROM post_views 
    GROUP BY post_id
) v
WHERE p.id = v.post_id;

-- 更新并返回
UPDATE users 
SET age = age + 1 
WHERE id = 1
RETURNING id, username, age;
```

### DELETE
```sql
-- 基本删除
DELETE FROM users WHERE id = 1;

-- 删除并返回
DELETE FROM users 
WHERE id = 1 
RETURNING *;

-- 子查询删除
DELETE FROM posts 
WHERE user_id IN (
    SELECT id FROM users WHERE is_active = false
);

-- 关联删除
DELETE FROM posts p
USING users u
WHERE p.user_id = u.id AND u.is_active = false;

-- 清空表 (重置自增计数器)
TRUNCATE TABLE posts RESTART IDENTITY CASCADE;
```

---

## DQL - 数据查询语言

### 基础查询
```sql
-- 查询所有列
SELECT * FROM users;

-- 查询指定列
SELECT id, username, email FROM users;

-- 去重查询
SELECT DISTINCT status FROM posts;

-- 别名
SELECT 
    u.id AS user_id,
    u.username AS 用户名,
    u.email AS 邮箱地址
FROM users u;

-- 条件查询
SELECT * FROM users WHERE age > 25;
SELECT * FROM users WHERE age BETWEEN 20 AND 30;
SELECT * FROM users WHERE username LIKE 'john%';
SELECT * FROM users WHERE email ILIKE '%@example.com';  -- 不区分大小写
SELECT * FROM users WHERE id IN (1, 2, 3);
SELECT * FROM users WHERE tags && ARRAY['admin', 'moderator'];  -- 数组交集

-- 空值判断
SELECT * FROM users WHERE full_name IS NULL;
SELECT * FROM users WHERE full_name IS NOT NULL;
SELECT * FROM users WHERE COALESCE(full_name, '') = '';

-- 逻辑运算符
SELECT * FROM users 
WHERE age >= 18 AND is_active = true;

SELECT * FROM users 
WHERE age < 18 OR age > 65;

SELECT * FROM users 
WHERE NOT is_active;
```

### 排序与分页
```sql
-- 排序
SELECT * FROM users ORDER BY created_at DESC;
SELECT * FROM users ORDER BY age ASC, username DESC;

-- 分页 (LIMIT/OFFSET)
SELECT * FROM users ORDER BY id LIMIT 10;
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 20;
SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 10 * 2;  -- 第3页

-- FETCH (SQL 标准语法)
SELECT * FROM users ORDER BY id 
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;
```

### 聚合函数
```sql
-- 基础聚合
SELECT 
    COUNT(*) AS total_users,
    COUNT(DISTINCT status) AS status_count,
    AVG(age) AS avg_age,
    MAX(age) AS max_age,
    MIN(age) AS min_age,
    SUM(view_count) AS total_views
FROM users;

-- 分组聚合
SELECT 
    status,
    COUNT(*) AS post_count,
    AVG(view_count) AS avg_views,
    MAX(created_at) AS last_post
FROM posts
GROUP BY status;

-- 分组过滤
SELECT 
    user_id,
    COUNT(*) AS post_count
FROM posts
GROUP BY user_id
HAVING COUNT(*) > 5;

-- 多维度分组
SELECT 
    DATE_TRUNC('month', created_at) AS month,
    status,
    COUNT(*) AS count
FROM posts
GROUP BY DATE_TRUNC('month', created_at), status
ORDER BY month DESC, count DESC;
```

### 连接查询
```sql
-- INNER JOIN
SELECT 
    u.username,
    p.title,
    p.status
FROM users u
INNER JOIN posts p ON u.id = p.user_id;

-- LEFT JOIN
SELECT 
    u.username,
    COUNT(p.id) AS post_count
FROM users u
LEFT JOIN posts p ON u.id = p.user_id
GROUP BY u.id, u.username;

-- RIGHT JOIN
SELECT 
    u.username,
    p.title
FROM users u
RIGHT JOIN posts p ON u.id = p.user_id;

-- FULL OUTER JOIN
SELECT 
    u.username,
    p.title
FROM users u
FULL OUTER JOIN posts p ON u.id = p.user_id;

-- 多表连接
SELECT 
    u.username,
    p.title,
    c.content AS comment_content
FROM users u
JOIN posts p ON u.id = p.user_id
LEFT JOIN comments c ON p.id = c.post_id;

-- 自连接
SELECT 
    e1.name AS employee,
    e2.name AS manager
FROM employees e1
LEFT JOIN employees e2 ON e1.manager_id = e2.id;

-- CROSS JOIN (笛卡尔积)
SELECT * FROM table1 CROSS JOIN table2;
-- 或
SELECT * FROM table1, table2;
```

### 子查询
```sql
-- 标量子查询
SELECT 
    username,
    (SELECT COUNT(*) FROM posts WHERE user_id = users.id) AS post_count
FROM users;

-- IN 子查询
SELECT * FROM users 
WHERE id IN (SELECT user_id FROM posts WHERE status = 'published');

-- EXISTS 子查询
SELECT * FROM users u
WHERE EXISTS (
    SELECT 1 FROM posts p 
    WHERE p.user_id = u.id AND p.view_count > 1000
);

-- NOT EXISTS
SELECT * FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM posts p WHERE p.user_id = u.id
);

-- 关联子查询
SELECT * FROM posts p1
WHERE view_count > (
    SELECT AVG(view_count) FROM posts p2 WHERE p2.user_id = p1.user_id
);

-- CTE (公用表表达式)
WITH active_users AS (
    SELECT id, username 
    FROM users 
    WHERE is_active = true
),
user_posts AS (
    SELECT 
        user_id, 
        COUNT(*) AS post_count,
        SUM(view_count) AS total_views
    FROM posts
    GROUP BY user_id
)
SELECT 
    au.username,
    COALESCE(up.post_count, 0) AS posts,
    COALESCE(up.total_views, 0) AS views
FROM active_users au
LEFT JOIN user_posts up ON au.id = up.user_id;

-- 递归 CTE
WITH RECURSIVE category_tree AS (
    -- 基础查询
    SELECT id, name, parent_id, 0 AS level
    FROM categories
    WHERE parent_id IS NULL
    
    UNION ALL
    
    -- 递归查询
    SELECT c.id, c.name, c.parent_id, ct.level + 1
    FROM categories c
    INNER JOIN category_tree ct ON c.parent_id = ct.id
)
SELECT * FROM category_tree;
```

### 窗口函数
```sql
-- ROW_NUMBER
SELECT 
    username,
    age,
    ROW_NUMBER() OVER (ORDER BY age DESC) AS age_rank
FROM users;

-- RANK / DENSE_RANK
SELECT 
    username,
    age,
    RANK() OVER (ORDER BY age DESC) AS rank,
    DENSE_RANK() OVER (ORDER BY age DESC) AS dense_rank
FROM users;

-- 分区窗口函数
SELECT 
    user_id,
    title,
    view_count,
    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY view_count DESC) AS post_rank
FROM posts;

-- LAG / LEAD
SELECT 
    username,
    created_at,
    LAG(created_at) OVER (ORDER BY created_at) AS prev_user_created,
    LEAD(created_at) OVER (ORDER BY created_at) AS next_user_created
FROM users;

-- FIRST_VALUE / LAST_VALUE
SELECT 
    user_id,
    title,
    view_count,
    FIRST_VALUE(title) OVER (
        PARTITION BY user_id 
        ORDER BY view_count DESC 
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS most_viewed_post
FROM posts;

-- 移动平均
SELECT 
    date,
    daily_sales,
    AVG(daily_sales) OVER (
        ORDER BY date 
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS moving_avg_7d
FROM sales;

-- 累计求和
SELECT 
    user_id,
    created_at,
    view_count,
    SUM(view_count) OVER (
        PARTITION BY user_id 
        ORDER BY created_at
    ) AS cumulative_views
FROM posts;
```

---

## 高级特性

### JSON/JSONB 操作
```sql
-- 创建 JSONB 数据
UPDATE users 
SET profile_data = '{
    "bio": "Software developer",
    "location": "Beijing",
    "social": {
        "github": "johndoe",
        "twitter": "@johndoe"
    },
    "skills": ["Python", "PostgreSQL", "JavaScript"]
}'::jsonb
WHERE id = 1;

-- 查询 JSONB
SELECT * FROM users 
WHERE profile_data @> '{"location": "Beijing"}'::jsonb;

SELECT * FROM users 
WHERE profile_data->>'bio' ILIKE '%developer%';

-- 提取 JSON 字段
SELECT 
    username,
    profile_data->>'bio' AS bio,
    profile_data->'social'->>'github' AS github,
    profile_data->'skills' AS skills
FROM users;

-- 数组元素查询
SELECT * FROM users 
WHERE profile_data->'skills' @> '["Python"]'::jsonb;

-- 更新 JSONB
UPDATE users 
SET profile_data = jsonb_set(
    profile_data, 
    '{social,linkedin}', 
    '"johndoe"'
)
WHERE id = 1;

-- 添加数组元素
UPDATE users 
SET profile_data = jsonb_set(
    profile_data,
    '{skills}',
    (profile_data->'skills') || '["Go"]'
)
WHERE id = 1;

-- 删除 JSON 字段
UPDATE users 
SET profile_data = profile_data - 'old_field'
WHERE id = 1;

-- JSONB 聚合
SELECT 
    jsonb_agg(profile_data) AS all_profiles,
    jsonb_object_agg(username, profile_data) AS user_profiles
FROM users;
```

### 数组操作
```sql
-- 数组类型
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    tags TEXT[],
    prices NUMERIC[],
    attributes TEXT[][]
);

-- 插入数组
INSERT INTO products (name, tags, prices)
VALUES (
    'Laptop',
    ARRAY['electronics', 'computers', 'portable'],
    ARRAY[999.99, 899.99, 799.99]
);

-- 数组查询
SELECT * FROM products WHERE tags @> ARRAY['electronics'];
SELECT * FROM products WHERE tags && ARRAY['computers', 'laptops'];
SELECT * FROM products WHERE 'electronics' = ANY(tags);

-- 数组操作
SELECT 
    name,
    array_length(tags, 1) AS tag_count,
    tags[1] AS first_tag,
    tags[1:2] AS first_two_tags,
    unnest(tags) AS tag
FROM products;

-- 数组聚合
SELECT 
    category_id,
    array_agg(name) AS product_names,
    string_agg(name, ', ') AS product_list
FROM products
GROUP BY category_id;
```

### 全文搜索
```sql
-- 创建搜索向量列
ALTER TABLE posts ADD COLUMN search_vector tsvector;

-- 更新搜索向量
UPDATE posts 
SET search_vector = 
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(content, '')), 'B');

-- 创建 GIN 索引
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);

-- 全文搜索查询
SELECT 
    id,
    title,
    ts_rank(search_vector, query) AS rank
FROM posts, 
    plainto_tsquery('english', 'postgresql tutorial') query
WHERE search_vector @@ query
ORDER BY rank DESC;

-- 高亮搜索结果
SELECT 
    id,
    title,
    ts_headline('english', content, query) AS highlighted
FROM posts,
    plainto_tsquery('english', 'database') query
WHERE search_vector @@ query;

-- 中文全文搜索 (需要额外配置)
-- 安装 zhparser 扩展后使用
```

### 事务控制
```sql
-- 基本事务
BEGIN;
    INSERT INTO users (username, email) VALUES ('user1', 'user1@test.com');
    INSERT INTO posts (user_id, title) VALUES (currval('users_id_seq'), 'First Post');
COMMIT;

-- 回滚事务
BEGIN;
    UPDATE accounts SET balance = balance - 100 WHERE id = 1;
    UPDATE accounts SET balance = balance + 100 WHERE id = 2;
    -- 检查余额
    IF (SELECT balance FROM accounts WHERE id = 1) < 0 THEN
        ROLLBACK;
    ELSE
        COMMIT;
    END IF;

-- 保存点
BEGIN;
    INSERT INTO users (username) VALUES ('user_a');
    SAVEPOINT sp1;
    INSERT INTO users (username) VALUES ('user_b');
    ROLLBACK TO SAVEPOINT sp1;  -- 撤销 user_b 的插入
    INSERT INTO users (username) VALUES ('user_c');
COMMIT;

-- 事务隔离级别
BEGIN ISOLATION LEVEL READ COMMITTED;
BEGIN ISOLATION LEVEL REPEATABLE READ;
BEGIN ISOLATION LEVEL SERIALIZABLE;
```

### 视图
```sql
-- 创建视图
CREATE VIEW active_posts AS
SELECT 
    p.id,
    p.title,
    p.content,
    u.username AS author,
    p.view_count,
    p.created_at
FROM posts p
JOIN users u ON p.user_id = u.id
WHERE p.status = 'published' AND u.is_active = true;

-- 查询视图
SELECT * FROM active_posts WHERE view_count > 100;

-- 物化视图
CREATE MATERIALIZED VIEW daily_stats AS
SELECT 
    DATE(created_at) AS date,
    COUNT(*) AS new_users,
    COUNT(*) FILTER (WHERE is_active) AS active_users
FROM users
GROUP BY DATE(created_at);

-- 刷新物化视图
REFRESH MATERIALIZED VIEW daily_stats;
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_stats;  -- 不阻塞查询

-- 创建索引
CREATE UNIQUE INDEX ON daily_stats (date);

-- 删除视图
DROP VIEW IF EXISTS active_posts;
DROP MATERIALIZED VIEW IF EXISTS daily_stats;
```

### 存储过程与函数
```sql
-- 创建函数
CREATE OR REPLACE FUNCTION get_user_post_count(user_id INTEGER)
RETURNS INTEGER AS $$
DECLARE
    post_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO post_count 
    FROM posts 
    WHERE posts.user_id = user_id;
    
    RETURN post_count;
END;
$$ LANGUAGE plpgsql;

-- 使用函数
SELECT username, get_user_post_count(id) AS posts FROM users;

-- 返回表的函数
CREATE OR REPLACE FUNCTION get_user_posts(p_user_id INTEGER)
RETURNS TABLE (
    post_id INTEGER,
    title VARCHAR,
    status VARCHAR,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id,
        p.title::VARCHAR,
        p.status::VARCHAR,
        p.created_at
    FROM posts p
    WHERE p.user_id = p_user_id
    ORDER BY p.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- 触发器函数
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 删除触发器
DROP TRIGGER IF EXISTS trigger_users_updated_at ON users;
```

---

## 性能优化

### 查询优化
```sql
-- EXPLAIN ANALYZE 分析查询
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT * FROM posts 
WHERE user_id = 1 AND status = 'published';

-- 覆盖索引
CREATE INDEX idx_posts_covering ON posts(user_id, status) 
INCLUDE (title, view_count);

-- 部分索引
CREATE INDEX idx_posts_recent ON posts(created_at) 
WHERE created_at > CURRENT_DATE - INTERVAL '30 days';

-- 表达式索引
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- 查询提示
SELECT /*+ INDEX(posts idx_posts_user_status) */ *
FROM posts 
WHERE user_id = 1;
```

### 表维护
```sql
-- 分析表
ANALYZE users;
ANALYZE VERBOSE posts;

-- 清理表
VACUUM posts;
VACUUM ANALYZE posts;
VACUUM FULL posts;  -- 重建表, 需要锁

-- 重建索引
REINDEX INDEX idx_users_email;
REINDEX TABLE users;

-- 查看表统计
SELECT 
    schemaname,
    tablename,
    n_live_tup AS live_rows,
    n_dead_tup AS dead_rows,
    last_vacuum,
    last_analyze
FROM pg_stat_user_tables
WHERE tablename = 'users';

-- 查看索引使用
SELECT 
    schemaname,
    tablename,
    indexrelname AS index_name,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read
FROM pg_stat_user_indexes
WHERE tablename = 'users';
```

### 配置优化
```sql
-- 查看当前配置
SHOW work_mem;
SHOW shared_buffers;
SHOW effective_cache_size;

-- 会话级设置
SET work_mem = '256MB';
SET LOCAL work_mem = '1GB';  -- 仅当前事务

-- 查看慢查询
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## 实用查询

### 数据库信息
```sql
-- 查看所有表
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 查看表结构
\d users
-- 或
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'users';

-- 查看索引
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'users';

-- 查看外键
SELECT
    tc.constraint_name,
    kcu.column_name,
    ccu.table_name AS foreign_table,
    ccu.column_name AS foreign_column
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu 
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage ccu 
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_name = 'posts';
```

### 用户和权限
```sql
-- 创建用户
CREATE USER app_user WITH PASSWORD 'secure_password';

-- 授权
GRANT CONNECT ON DATABASE mydb TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- 撤销权限
REVOKE INSERT ON posts FROM app_user;

-- 查看权限
\dp
-- 或
SELECT * FROM information_schema.table_privileges 
WHERE grantee = 'app_user';
```

---

## 总结

PostgreSQL 是一个功能强大的开源关系型数据库，主要特点包括：

1. **丰富的数据类型** - 支持 JSONB、数组、地理空间等高级类型
2. **强大的扩展性** - 支持自定义类型、函数、存储过程
3. **高级索引** - B-tree、Hash、GiST、SP-GiST、GIN、BRIN 等
4. **并发控制** - MVCC 实现高效的并发处理
5. **分区表** - 支持表分区优化大数据量查询
6. **全文搜索** - 内置强大的全文搜索功能
7. **窗口函数** - 支持复杂的分析查询
8. **CTE 和递归查询** - 处理层次数据

更多详情请参考 [PostgreSQL 官方文档](https://www.postgresql.org/docs/)。
