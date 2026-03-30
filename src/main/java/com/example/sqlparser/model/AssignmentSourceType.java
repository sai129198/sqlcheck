package com.example.sqlparser.model;

/**
 * 赋值来源类型
 */
public enum AssignmentSourceType {
    CONSTANT,
    EXPRESSION,
    SUBQUERY,
    DEFAULT,
    FROM_TABLE,
    EXCLUDED
}
