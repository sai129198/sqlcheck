package com.example.sqlparser.model;

import lombok.Data;

/**
 * JOIN 条件
 */
@Data
public class JoinCondition {
    private JoinType joinType;
    private TableRef rightTable;
    private LineageInfo condition;
}
