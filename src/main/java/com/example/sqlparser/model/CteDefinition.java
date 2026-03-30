package com.example.sqlparser.model;

import lombok.Data;

/**
 * CTE 定义
 */
@Data
public class CteDefinition {
    private String name;
    private java.util.List<String> columnNames;
    private QueryBlock query;
}
