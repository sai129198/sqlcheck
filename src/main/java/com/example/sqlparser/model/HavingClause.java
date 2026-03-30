package com.example.sqlparser.model;

import lombok.Data;

/**
 * HAVING 子句
 */
@Data
public class HavingClause {
    private LineageInfo conditionLineage;
}
