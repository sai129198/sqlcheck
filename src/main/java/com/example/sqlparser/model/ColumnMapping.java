package com.example.sqlparser.model;

import lombok.Data;

/**
 * 字段映射
 */
@Data
public class ColumnMapping {
    private String targetColumn;
    private LineageInfo sourceLineage;
}
