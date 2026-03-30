package com.example.sqlparser.model;

/**
 * 查询块类型
 */
public enum QueryBlockType {
    SIMPLE_SELECT,
    UNION,
    INTERSECT,
    EXCEPT
}
