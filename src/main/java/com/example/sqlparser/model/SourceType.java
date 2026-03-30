package com.example.sqlparser.model;

/**
 * 血缘来源类型
 */
public enum SourceType {
    TABLE_COLUMN,
    CTE_COLUMN,
    SUBQUERY_COLUMN,
    CONSTANT,
    EXPRESSION,
    UNRESOLVED
}
