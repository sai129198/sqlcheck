package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * UPDATE 语句详情
 */
@Data
public class UpdateDetails {
    private TargetTable targetTable;
    private List<Assignment> assignments = new ArrayList<>();
    private FromClause fromClause;
    private List<JoinCondition> joins = new ArrayList<>();
    private WhereClause where;
    private OrderByClause orderBy;
    private LimitClause limit;
}
