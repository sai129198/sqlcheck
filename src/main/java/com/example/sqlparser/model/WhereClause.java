package com.example.sqlparser.model;

import lombok.Data;

/**
 * WHERE 子句
 */
@Data
public class WhereClause {
    private LineageInfo conditionLineage;
}
