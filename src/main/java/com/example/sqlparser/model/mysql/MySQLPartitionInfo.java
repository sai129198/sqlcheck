package com.example.sqlparser.model.mysql;

import lombok.Data;
import java.util.List;

/**
 * MySQL 分区信息
 */
@Data
public class MySQLPartitionInfo {
    
    private PartitionType type;
    private List<String> partitionColumns;
    private Integer partitionCount;
    private List<MySQLPartition> partitions;
    
    // 子分区
    private SubPartitionInfo subPartitionInfo;
    
    public enum PartitionType {
        RANGE,
        LIST,
        HASH,
        KEY,
        RANGE_COLUMNS,
        LIST_COLUMNS
    }
    
    @Data
    public static class MySQLPartition {
        private String name;
        private String values;  // VALUES LESS THAN (...) 或 VALUES IN (...)
        private String comment;
        private String dataDirectory;
        private String indexDirectory;
    }
    
    @Data
    public static class SubPartitionInfo {
        private SubPartitionType type;
        private Integer subPartitionCount;
        
        public enum SubPartitionType {
            HASH,
            KEY
        }
    }
}
