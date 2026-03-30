package com.example.sqlparser.model;

/**
 * 子查询类型
 */
public enum SubQueryType {
    IN,
    EXISTS,
    SCALAR,
    COMPARISON,
    DERIVED_TABLE
}
