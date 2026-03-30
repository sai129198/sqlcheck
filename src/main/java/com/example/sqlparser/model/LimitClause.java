package com.example.sqlparser.model;

import lombok.Data;

/**
 * LIMIT 子句
 */
@Data
public class LimitClause {
    private long limit;
    private long offset;
}
