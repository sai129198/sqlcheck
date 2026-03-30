package com.example.sqlparser.model;

import lombok.Data;

/**
 * 子查询引用
 */
@Data
public class SubQueryRef {
    private QueryBlock query;
    private SubQueryType type;
    private String alias;
}
