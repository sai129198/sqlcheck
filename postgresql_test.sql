-- ============================================
-- PostgreSQL 测试文件
-- 包含完整的测试用例和示例数据
-- ============================================

-- 清理环境
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS post_views CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS sales CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS events_2024_q1 CASCADE;
DROP TABLE IF EXISTS events_2024_q2 CASCADE;
DROP TABLE IF EXISTS events_2024_q3 CASCADE;
DROP TABLE IF EXISTS events_2024_q4 CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP MATERIALIZED VIEW IF EXISTS daily_stats CASCADE;
DROP VIEW IF EXISTS active_posts CASCADE;
DROP FUNCTION IF EXISTS get_user_post_count(INTEGER) CASCADE;
DROP FUNCTION IF EXISTS get_user_posts(INTEGER) CASCADE;
DROP FUNCTION IF EXISTS update_updated_at() CASCADE;

-- ============================================
-- 1. 基础表创建测试
-- ============================================

-- 创建枚举类型
CREATE TYPE user_role AS ENUM ('admin', 'moderator', 'user', 'guest');
CREATE TYPE order_status AS ENUM ('pending', 'processing', 'completed', 'cancelled');

-- 创建用户表
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

-- 创建角色表
CREATE TABLE user_roles (
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    role user_role NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role)
);

-- 创建文章表
CREATE TABLE posts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(20) DEFAULT 'draft' 
        CHECK (status IN ('draft', 'published', 'archived')),
    view_count INTEGER DEFAULT 0,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    search_vector tsvector
);

-- 创建评论表
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建文章浏览记录表
CREATE TABLE post_views (
    id SERIAL PRIMARY KEY,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    viewer_ip INET,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建产品表 (数组类型测试)
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    tags TEXT[],
    prices NUMERIC(10,2)[],
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建分类表 (递归查询测试)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INTEGER REFERENCES categories(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建账户表 (事务测试)
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    account_number VARCHAR(50) UNIQUE NOT NULL,
    balance NUMERIC(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建员工表 (自连接测试)
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    manager_id INTEGER REFERENCES employees(id),
    department VARCHAR(50),
    salary NUMERIC(10,2),
    hired_at DATE
);

-- 创建销售表 (窗口函数测试)
CREATE TABLE sales (
    id SERIAL PRIMARY KEY,
    product_name VARCHAR(100),
    amount NUMERIC(10,2),
    sale_date DATE NOT NULL
);

-- ============================================
-- 2. 分区表测试
-- ============================================

-- 创建分区表
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

-- ============================================
-- 3. 索引创建测试
-- ============================================

-- B-tree 索引
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_status ON posts(status);

-- 复合索引
CREATE INDEX idx_posts_user_status ON posts(user_id, status);

-- 部分索引
CREATE INDEX idx_posts_published ON posts(created_at) 
    WHERE status = 'published';

-- 表达式索引
CREATE UNIQUE INDEX idx_users_username_lower ON users(LOWER(username));
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- GIN 索引 (JSONB 和数组)
CREATE INDEX idx_users_profile ON users USING GIN(profile_data);
CREATE INDEX idx_users_tags ON users USING GIN(tags);
CREATE INDEX idx_products_metadata ON products USING GIN(metadata);
CREATE INDEX idx_products_tags ON products USING GIN(tags);

-- 全文搜索索引
CREATE INDEX idx_posts_search ON posts USING GIN(search_vector);

-- ============================================
-- 4. 插入测试数据
-- ============================================

-- 插入用户数据
INSERT INTO users (username, email, password_hash, full_name, age, is_active, profile_data, tags) VALUES
    ('admin_user', 'admin@example.com', 'hash_admin', 'Administrator', 35, true, '{"role": "admin", "department": "IT"}'::jsonb, ARRAY['admin', 'staff']),
    ('john_doe', 'john@example.com', 'hash_john', 'John Doe', 30, true, '{"bio": "Software developer", "location": "Beijing"}'::jsonb, ARRAY['developer', 'user']),
    ('jane_smith', 'jane@example.com', 'hash_jane', 'Jane Smith', 28, true, '{"bio": "Data analyst", "location": "Shanghai"}'::jsonb, ARRAY['analyst', 'user']),
    ('bob_wilson', 'bob@example.com', 'hash_bob', 'Bob Wilson', 45, true, '{"bio": "Manager", "location": "Guangzhou"}'::jsonb, ARRAY['manager', 'user']),
    ('alice_brown', 'alice@example.com', 'hash_alice', 'Alice Brown', 25, false, '{"bio": "Intern", "location": "Shenzhen"}'::jsonb, ARRAY['intern']),
    ('charlie_davis', 'charlie@example.com', 'hash_charlie', 'Charlie Davis', 32, true, '{"bio": "DevOps engineer", "location": "Beijing"}'::jsonb, ARRAY['devops', 'user']),
    ('diana_evans', 'diana@example.com', 'hash_diana', 'Diana Evans', 29, true, '{"bio": "Designer", "location": "Shanghai"}'::jsonb, ARRAY['designer', 'user']),
    ('eve_foster', 'eve@example.com', 'hash_eve', 'Eve Foster', 38, true, '{"bio": "Product manager", "location": "Beijing"}'::jsonb, ARRAY['pm', 'user']);

-- 插入角色数据
INSERT INTO user_roles (user_id, role) VALUES
    (1, 'admin'),
    (2, 'user'),
    (3, 'user'),
    (4, 'moderator'),
    (4, 'user'),
    (5, 'guest'),
    (6, 'user'),
    (7, 'user'),
    (8, 'user');

-- 插入文章数据
INSERT INTO posts (user_id, title, content, status, view_count, published_at) VALUES
    (1, 'Welcome to PostgreSQL', 'PostgreSQL is a powerful, open source object-relational database system.', 'published', 1500, '2024-01-15 10:00:00'),
    (2, 'Getting Started with SQL', 'SQL is the standard language for relational database management.', 'published', 2300, '2024-01-20 14:30:00'),
    (2, 'Advanced Query Techniques', 'Learn advanced SQL queries including window functions and CTEs.', 'published', 890, '2024-02-05 09:15:00'),
    (3, 'Data Analysis with PostgreSQL', 'How to perform data analysis using PostgreSQL analytical functions.', 'published', 1200, '2024-02-10 16:00:00'),
    (4, 'Database Performance Tips', 'Best practices for optimizing PostgreSQL performance.', 'draft', 0, NULL),
    (6, 'DevOps and Databases', 'Managing databases in a DevOps environment.', 'published', 560, '2024-03-01 11:00:00'),
    (7, 'UI/UX Design Principles', 'Design principles for better user experience.', 'archived', 320, '2023-12-01 10:00:00'),
    (8, 'Product Management 101', 'Introduction to product management.', 'published', 780, '2024-03-10 13:30:00'),
    (2, 'Draft Post', 'This is a draft post not yet published.', 'draft', 0, NULL),
    (3, 'Another Published Post', 'More content for testing.', 'published', 450, '2024-03-15 15:00:00');

-- 更新搜索向量
UPDATE posts SET search_vector = 
    setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(content, '')), 'B');

-- 插入评论数据
INSERT INTO comments (post_id, user_id, content) VALUES
    (1, 2, 'Great introduction!'),
    (1, 3, 'Very helpful, thanks!'),
    (2, 4, 'Good basics coverage.'),
    (2, 5, 'Could use more examples.'),
    (3, 6, 'Advanced techniques explained well.'),
    (4, 7, 'Love the analytical approach.'),
    (6, 8, 'DevOps perspective is valuable.');

-- 插入浏览记录
INSERT INTO post_views (post_id, viewer_ip, viewed_at) VALUES
    (1, '192.168.1.100'::inet, '2024-03-20 10:00:00'),
    (1, '192.168.1.101'::inet, '2024-03-20 10:05:00'),
    (1, '192.168.1.102'::inet, '2024-03-20 10:10:00'),
    (2, '192.168.1.100'::inet, '2024-03-20 11:00:00'),
    (2, '192.168.1.103'::inet, '2024-03-20 11:30:00'),
    (3, '192.168.1.104'::inet, '2024-03-20 12:00:00');

-- 插入产品数据
INSERT INTO products (name, description, tags, prices, metadata) VALUES
    ('Laptop Pro', 'High-performance laptop for professionals', 
     ARRAY['electronics', 'computers', 'laptops'], 
     ARRAY[1299.99, 1199.99, 1099.99],
     '{"brand": "TechCorp", "warranty": "2 years", "specs": {"cpu": "Intel i7", "ram": "16GB"}}'::jsonb),
    ('Wireless Mouse', 'Ergonomic wireless mouse', 
     ARRAY['electronics', 'accessories'], 
     ARRAY[49.99, 39.99],
     '{"brand": "MouseInc", "color": "black"}'::jsonb),
    ('Mechanical Keyboard', 'RGB mechanical keyboard', 
     ARRAY['electronics', 'accessories', 'gaming'], 
     ARRAY[129.99, 99.99],
     '{"brand": "KeyMaster", "switches": "Cherry MX"}'::jsonb),
    ('Monitor 4K', '27-inch 4K display', 
     ARRAY['electronics', 'displays'], 
     ARRAY[599.99, 499.99],
     '{"brand": "ViewPro", "resolution": "3840x2160"}'::jsonb);

-- 插入分类数据 (树形结构)
INSERT INTO categories (name, parent_id) VALUES
    ('Electronics', NULL),
    ('Computers', 1),
    ('Laptops', 2),
    ('Desktops', 2),
    ('Accessories', 1),
    ('Keyboards', 5),
    ('Mice', 5),
    ('Clothing', NULL),
    ('Men', 8),
    ('Women', 8);

-- 插入账户数据
INSERT INTO accounts (user_id, account_number, balance) VALUES
    (1, 'ACC-001-2024', 10000.00),
    (2, 'ACC-002-2024', 5000.00),
    (3, 'ACC-003-2024', 7500.00),
    (4, 'ACC-004-2024', 15000.00),
    (6, 'ACC-006-2024', 3000.00);

-- 插入员工数据 (层次结构)
INSERT INTO employees (name, email, manager_id, department, salary, hired_at) VALUES
    ('CEO Smith', 'ceo@company.com', NULL, 'Executive', 200000.00, '2020-01-15'),
    ('CTO Johnson', 'cto@company.com', 1, 'Technology', 150000.00, '2020-02-01'),
    ('CFO Williams', 'cfo@company.com', 1, 'Finance', 150000.00, '2020-02-01'),
    ('Dev Manager Brown', 'dev.mgr@company.com', 2, 'Technology', 120000.00, '2020-03-15'),
    ('Senior Dev Davis', 'senior.dev@company.com', 4, 'Technology', 100000.00, '2020-06-01'),
    ('Junior Dev Miller', 'junior.dev@company.com', 4, 'Technology', 60000.00, '2021-01-15'),
    ('QA Lead Wilson', 'qa.lead@company.com', 2, 'Technology', 90000.00, '2020-08-01'),
    ('Accountant Jones', 'accountant@company.com', 3, 'Finance', 70000.00, '2020-09-01');

-- 插入销售数据
INSERT INTO sales (product_name, amount, sale_date) VALUES
    ('Product A', 1000.00, '2024-01-01'),
    ('Product B', 1500.00, '2024-01-02'),
    ('Product A', 1200.00, '2024-01-03'),
    ('Product C', 800.00, '2024-01-04'),
    ('Product B', 2000.00, '2024-01-05'),
    ('Product A', 900.00, '2024-01-06'),
    ('Product C', 1100.00, '2024-01-07'),
    ('Product A', 1300.00, '2024-01-08'),
    ('Product B', 1700.00, '2024-01-09'),
    ('Product C', 950.00, '2024-01-10');

-- 插入事件数据 (分区表)
INSERT INTO events (event_type, event_data, created_at) VALUES
    ('user_login', '{"user_id": 1, "ip": "192.168.1.100"}'::jsonb, '2024-01-15 10:00:00'),
    ('user_logout', '{"user_id": 1}'::jsonb, '2024-01-15 18:00:00'),
    ('purchase', '{"user_id": 2, "amount": 99.99}'::jsonb, '2024-02-20 14:30:00'),
    ('user_login', '{"user_id": 3, "ip": "192.168.1.101"}'::jsonb, '2024-03-10 09:00:00'),
    ('page_view', '{"page": "/home", "user_id": 2}'::jsonb, '2024-03-25 16:00:00');

-- ============================================
-- 5. 创建视图
-- ============================================

-- 创建活动文章视图
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

-- 创建物化视图
CREATE MATERIALIZED VIEW daily_stats AS
SELECT 
    DATE(created_at) AS date,
    COUNT(*) AS new_users,
    COUNT(*) FILTER (WHERE is_active) AS active_users
FROM users
GROUP BY DATE(created_at);

CREATE UNIQUE INDEX ON daily_stats (date);

-- ============================================
-- 6. 创建函数和触发器
-- ============================================

-- 创建更新时间的触发器函数
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为用户表创建触发器
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 为文章表创建触发器
CREATE TRIGGER trigger_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- 创建获取用户文章数的函数
CREATE OR REPLACE FUNCTION get_user_post_count(p_user_id INTEGER)
RETURNS INTEGER AS $$
DECLARE
    post_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO post_count 
    FROM posts 
    WHERE user_id = p_user_id;
    
    RETURN post_count;
END;
$$ LANGUAGE plpgsql;

-- 创建返回用户文章的函数
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

-- ============================================
-- 7. 测试查询
-- ============================================

-- 7.1 基础查询测试
SELECT '=== 基础查询测试 ===' AS section;

-- 查询所有用户
SELECT id, username, email, full_name, age FROM users ORDER BY id;

-- 条件查询
SELECT username, email FROM users WHERE is_active = true AND age > 25;

-- 模糊查询
SELECT username, email FROM users WHERE email LIKE '%@example.com';

-- 7.2 连接查询测试
SELECT '=== 连接查询测试 ===' AS section;

-- INNER JOIN
SELECT u.username, p.title, p.status
FROM users u
INNER JOIN posts p ON u.id = p.user_id
WHERE p.status = 'published'
ORDER BY u.username;

-- LEFT JOIN 统计用户文章数
SELECT 
    u.username,
    COUNT(p.id) AS post_count,
    COALESCE(SUM(p.view_count), 0) AS total_views
FROM users u
LEFT JOIN posts p ON u.id = p.user_id AND p.status = 'published'
GROUP BY u.id, u.username
ORDER BY post_count DESC;

-- 7.3 聚合函数测试
SELECT '=== 聚合函数测试 ===' AS section;

-- 统计信息
SELECT 
    status,
    COUNT(*) AS post_count,
    AVG(view_count) AS avg_views,
    MAX(view_count) AS max_views,
    MIN(view_count) AS min_views,
    SUM(view_count) AS total_views
FROM posts
GROUP BY status;

-- 7.4 窗口函数测试
SELECT '=== 窗口函数测试 ===' AS section;

-- 排名
SELECT 
    username,
    age,
    ROW_NUMBER() OVER (ORDER BY age DESC) AS age_rank,
    RANK() OVER (ORDER BY age DESC) AS rank,
    DENSE_RANK() OVER (ORDER BY age DESC) AS dense_rank
FROM users
ORDER BY age_rank;

-- 累计求和
SELECT 
    sale_date,
    amount,
    SUM(amount) OVER (ORDER BY sale_date) AS cumulative_sales,
    AVG(amount) OVER (ORDER BY sale_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg_3d
FROM sales
ORDER BY sale_date;

-- 7.5 CTE 测试
SELECT '=== CTE 测试 ===' AS section;

-- 递归 CTE 查询分类树
WITH RECURSIVE category_tree AS (
    SELECT id, name, parent_id, 0 AS level, name AS path
    FROM categories
    WHERE parent_id IS NULL
    
    UNION ALL
    
    SELECT c.id, c.name, c.parent_id, ct.level + 1, ct.path || ' > ' || c.name
    FROM categories c
    INNER JOIN category_tree ct ON c.parent_id = ct.id
)
SELECT id, name, level, path FROM category_tree ORDER BY path;

-- 7.6 JSONB 操作测试
SELECT '=== JSONB 操作测试 ===' AS section;

-- JSONB 查询
SELECT 
    username,
    profile_data->>'bio' AS bio,
    profile_data->>'location' AS location
FROM users
WHERE profile_data @> '{"location": "Beijing"}'::jsonb;

-- 7.7 数组操作测试
SELECT '=== 数组操作测试 ===' AS section;

-- 数组查询
SELECT name, tags FROM products WHERE tags @> ARRAY['electronics'];

-- 数组展开
SELECT name, unnest(tags) AS tag FROM products;

-- 7.8 全文搜索测试
SELECT '=== 全文搜索测试 ===' AS section;

-- 全文搜索
SELECT 
    id,
    title,
    ts_rank(search_vector, query) AS rank
FROM posts, 
    plainto_tsquery('english', 'postgresql tutorial') query
WHERE search_vector @@ query
ORDER BY rank DESC;

-- 7.9 函数测试
SELECT '=== 函数测试 ===' AS section;

-- 测试获取用户文章数函数
SELECT username, get_user_post_count(id) AS post_count FROM users ORDER BY post_count DESC;

-- 测试获取用户文章函数
SELECT * FROM get_user_posts(2);

-- ============================================
-- 8. 事务测试
-- ============================================

-- 事务示例 (注释掉，需要时手动执行)
/*
BEGIN;
    -- 转账操作
    UPDATE accounts SET balance = balance - 1000.00 WHERE id = 1;
    UPDATE accounts SET balance = balance + 1000.00 WHERE id = 2;
    
    -- 检查余额是否充足
    IF (SELECT balance FROM accounts WHERE id = 1) < 0 THEN
        ROLLBACK;
    ELSE
        COMMIT;
    END IF;
*/

-- ============================================
-- 9. UPSERT 测试
-- ============================================

-- 插入或更新
INSERT INTO users (id, username, email, password_hash)
VALUES (1, 'admin_updated', 'admin_new@example.com', 'new_hash')
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    updated_at = CURRENT_TIMESTAMP
RETURNING *;

-- 恢复原始数据
UPDATE users SET username = 'admin_user', email = 'admin@example.com' WHERE id = 1;

-- ============================================
-- 10. 性能分析查询
-- ============================================

-- 查看表大小
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 查看索引使用情况
SELECT 
    schemaname,
    tablename,
    indexrelname AS index_name,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- ============================================
-- 测试完成
-- ============================================

SELECT '所有测试已完成！' AS message;
