package com.example.sqlparser.model;

import lombok.Data;

/**
 * 列血缘（用于统一接口）
 */
@Data
public class ColumnLineage {
    private String targetColumn;
    private LineageInfo sourceLineage;

    public ColumnLineage(String targetColumn, LineageInfo sourceLineage) {
        this.targetColumn = targetColumn;
        this.sourceLineage = sourceLineage;
    }
}
