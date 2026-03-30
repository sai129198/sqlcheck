package com.example.sqlparser.model;

import lombok.Data;

/**
 * 目标表
 */
@Data
public class TargetTable {
    private String tableName;
    private String alias;
    private String schema;
    private TableType type;
    private boolean partitioned;
    private String partitionName;
}
