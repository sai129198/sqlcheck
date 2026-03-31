package com.example.sqlparser.model.postgresql;

import lombok.Data;
import java.util.List;

/**
 * PostgreSQL CREATE TABLE 语句
 */
@Data
public class PostgreSQLCreateTable {
    
    private boolean temporary;
    private boolean unlogged;
    private boolean ifNotExists;
    
    private String tableName;
    private String schema;
    
    // 列定义
    private List<PostgreSQLColumnDef> columns;
    
    // 约束
    private List<PostgreSQLConstraintDef> constraints;
    
    // 索引
    private List<PostgreSQLIndexDef> indexes;
    
    // 表选项
    private String tablespace;
    private String comment;
    private List<String> inherits;  // 继承的表
    
    // 分区
    private PostgreSQLPartitionInfo partitionInfo;
    
    // AS SELECT
    private String asSelect;
    private boolean withData = true;  // WITH DATA / WITH NO DATA
    
    // LIKE 复制表
    private String likeTable;
    private List<String> likeOptions;  // INCLUDING ...
    
    @Data
    public static class PostgreSQLIndexDef {
        private String name;
        private String type;  // PRIMARY KEY, UNIQUE, INDEX, GIN, GIST, BRIN, HASH, SP-GIST
        private List<String> columns;
        private String whereClause;  // 部分索引条件
        private String comment;
    }
    
    @Data
    public static class PostgreSQLConstraintDef {
        private String name;
        private String type;  // PRIMARY KEY, UNIQUE, CHECK, FOREIGN KEY, EXCLUDE
        private List<String> columns;
        private String expression;  // CHECK 表达式
        private String refTable;    // 外键引用表
        private List<String> refColumns;  // 外键引用列
        private String onDelete;    // ON DELETE 动作
        private String onUpdate;    // ON UPDATE 动作
    }
}
