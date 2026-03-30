package com.example.sqlparser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.mysql.*;
import com.example.sqlparser.model.presto.*;
import com.example.sqlparser.model.spark.*;
import com.example.sqlparser.parser.*;

/**
 * 演示如何提取 LIMIT 和 OFFSET
 */
public class LimitOffsetDemo {

    public static void main(String[] args) {
        System.out.println("========== LIMIT/OFFSET 提取演示 ==========\n");
        
        // 1. 基础 SQL 解析器 - 标准 LIMIT
        demoStandardLimit();
        
        // 2. MySQL 解析器 - LIMIT offset,count
        demoMySQLLimit();
        
        // 3. Presto 解析器 - OFFSET + FETCH FIRST
        demoPrestoLimit();
        
        // 4. Spark 解析器 - LIMIT
        demoSparkLimit();
        
        System.out.println("\n========== 演示完成 ==========");
    }
    
    private static void demoStandardLimit() {
        System.out.println("--- 1. 标准 LIMIT (基础解析器) ---");
        String sql = "SELECT id, name FROM users WHERE status = 'active' ORDER BY id LIMIT 10";
        System.out.println("SQL: " + sql);
        
        SqlParser parser = new SqlParser();
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        if (query.getLimit() != null) {
            System.out.println("LIMIT: " + query.getLimit().getLimit());
            System.out.println("OFFSET: " + query.getLimit().getOffset());
        } else {
            System.out.println("无 LIMIT 子句");
        }
        System.out.println();
    }
    
    private static void demoMySQLLimit() {
        System.out.println("--- 2. MySQL LIMIT offset,count ---");
        String sql = "SELECT * FROM users LIMIT 20, 10";
        System.out.println("SQL: " + sql);
        
        MySQLSqlParser parser = new MySQLSqlParser();
        SqlStatement stmt = parser.parse(sql);
        
        MySQLSelectDetails details = (MySQLSelectDetails) stmt.getSelectDetails();
        System.out.println("LIMIT Offset: " + details.getLimitOffset());
        System.out.println("LIMIT Count: " + details.getLimitCount());
        System.out.println();
    }
    
    private static void demoPrestoLimit() {
        System.out.println("--- 3. Presto OFFSET + FETCH FIRST ---");
        String sql = "SELECT * FROM users OFFSET 100 FETCH FIRST 10 ROWS ONLY";
        System.out.println("SQL: " + sql);
        
        PrestoSqlParser parser = new PrestoSqlParser();
        SqlStatement stmt = parser.parse(sql);
        
        PrestoSelectDetails details = (PrestoSelectDetails) stmt.getSelectDetails();
        System.out.println("OFFSET: " + details.getOffset());
        if (details.getFetchFirst() != null) {
            System.out.println("FETCH FIRST: " + details.getFetchFirst().getCount());
        }
        System.out.println();
    }
    
    private static void demoSparkLimit() {
        System.out.println("--- 4. Spark LIMIT ---");
        String sql = "SELECT * FROM users DISTRIBUTE BY dept SORT BY id LIMIT 100";
        System.out.println("SQL: " + sql);
        
        SparkSqlParser parser = new SparkSqlParser();
        SqlStatement stmt = parser.parse(sql);
        
        QueryBlock query = stmt.getSelectDetails().getQueryBlock();
        if (query.getLimit() != null) {
            System.out.println("LIMIT: " + query.getLimit().getLimit());
        }
        
        SparkSelectDetails details = (SparkSelectDetails) stmt.getSelectDetails();
        System.out.println("DISTRIBUTE BY: " + details.getDistributeBy());
        System.out.println("SORT BY: " + details.getSortBy());
        System.out.println();
    }
}
