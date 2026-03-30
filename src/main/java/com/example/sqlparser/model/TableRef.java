package com.example.sqlparser.model;

import lombok.Data;

/**
 * 表引用
 */
@Data
public class TableRef {
    private TableType type;
    private String name;
    private String alias;
    private String schema;
    
    // CTE 关联
    private CteDefinition cteRef;
    
    // 子查询关联
    private SubQueryRef subQueryRef;
    
    // 分区信息
    private boolean partitioned;
    private String partitionName;
}
