package com.example.sqlparser.model;

import lombok.Data;

/**
 * MERGE 语句详情（占位）
 */
@Data
public class MergeDetails {
    private TargetTable targetTable;
    private TableRef sourceTable;
    private LineageInfo mergeCondition;
}
