package com.example.sqlparser.model.postgresql;

import lombok.Data;

/**
 * PostgreSQL 分区信息
 */
@Data
public class PostgreSQLPartitionInfo {
    
    private String partitionType;  // RANGE, LIST, HASH
    private String partitionKey;   // 分区键表达式
    
    // 分区表特有
    private boolean partitioned;
    private String partitionBound; // 分区边界 (FOR VALUES ...)
    
    // 默认分区
    private boolean defaultPartition;
}
