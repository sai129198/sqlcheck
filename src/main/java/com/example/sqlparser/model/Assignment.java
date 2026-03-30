package com.example.sqlparser.model;

import lombok.Data;

/**
 * 赋值操作
 */
@Data
public class Assignment {
    private String targetColumn;
    private LineageInfo valueLineage;
    private AssignmentSourceType sourceType;
}
