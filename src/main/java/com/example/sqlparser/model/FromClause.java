package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * FROM 子句
 */
@Data
public class FromClause {
    private List<TableRef> tables = new ArrayList<>();
    private List<JoinCondition> joins = new ArrayList<>();
}
