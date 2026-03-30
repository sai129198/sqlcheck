package com.example.sqlparser.model.hive;

import lombok.Data;
import java.util.List;

/**
 * Hive CREATE TABLE 语句
 */
@Data
public class HiveCreateTable {
    
    private boolean external;
    private boolean temporary;
    private boolean ifNotExists;
    
    private String tableName;
    private String database;
    
    // 列定义
    private List<HiveColumnDef> columns;
    
    // 分区列
    private List<HiveColumnDef> partitionColumns;
    
    // 分桶
    private List<String> clusteredBy;
    private Integer numBuckets;
    private List<String> sortedBy;
    
    // 行格式
    private String rowFormat;
    private String fieldDelim;
    private String collectionDelim;
    private String mapKeyDelim;
    private String lineDelim;
    
    // 存储格式
    private String storedAs;
    private String inputFormat;
    private String outputFormat;
    
    // 位置
    private String location;
    
    // 表属性
    private List<Property> properties;
    
    // 表注释
    private String comment;
    
    // AS SELECT
    private String asSelect;
    
    // LIKE
    private String likeTable;
    
    @Data
    public static class Property {
        private String key;
        private String value;
    }
}
