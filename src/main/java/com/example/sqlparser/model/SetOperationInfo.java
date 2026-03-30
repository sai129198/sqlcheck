package com.example.sqlparser.model;

import lombok.Data;

/**
 * 集合操作信息
 */
@Data
public class SetOperationInfo {
    private SetOperationType type;
    private boolean all;
    private QueryBlock left;
    private QueryBlock right;
    private OrderByClause orderBy;
    private LimitClause limit;
}
