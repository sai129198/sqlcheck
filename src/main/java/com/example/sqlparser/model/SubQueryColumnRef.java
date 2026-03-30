package com.example.sqlparser.model;

import lombok.Data;

/**
 * 子查询字段引用
 */
@Data
public class SubQueryColumnRef {
    private QueryBlock subQuery;
    private String columnName;
    private LineageInfo sourceLineage;
}
