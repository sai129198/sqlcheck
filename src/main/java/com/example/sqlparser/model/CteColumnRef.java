package com.example.sqlparser.model;

import lombok.Data;

/**
 * CTE 字段引用
 */
@Data
public class CteColumnRef {
    private String cteName;
    private String columnName;
    private CteDefinition cteRef;
}
