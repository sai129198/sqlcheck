package com.example.sqlparser.model;

/**
 * 表达式类型
 */
public enum ExpressionType {
    COLUMN,
    CONSTANT,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    GREATER_THAN,
    LESS_EQUAL,
    GREATER_EQUAL,
    BETWEEN,
    IN,
    AND,
    OR,
    NOT,
    FUNCTION,
    AGGREGATE,
    WINDOW_FUNCTION,
    CASE,
    SUBQUERY,
    COALESCE,
    CAST,
    COMPOUND
}
