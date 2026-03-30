package com.example.sqlparser.model.mysql;

import lombok.Data;
import java.util.List;

/**
 * MySQL CREATE TABLE 语句
 */
@Data
public class MySQLCreateTable {
    
    private boolean temporary;
    private boolean ifNotExists;
    
    private String tableName;
    private String database;
    
    // 列定义
    private List<MySQLColumnDef> columns;
    
    // 索引
    private List<MySQLIndexDef> indexes;
    
    // 表选项
    private String engine;
    private String charset;
    private String collation;
    private Long autoIncrement;
    private String comment;
    
    // 分区
    private MySQLPartitionInfo partitionInfo;
    
    // AS SELECT
    private String asSelect;
    
    // LIKE 复制表
    private String likeTable;
    
    @Data
    public static class MySQLIndexDef {
        private String name;
        private String type;  // PRIMARY KEY, UNIQUE, INDEX, FULLTEXT, SPATIAL
        private List<String> columns;
        private String comment;
    }
}
