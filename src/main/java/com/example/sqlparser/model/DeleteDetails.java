package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DELETE 语句详情
 */
@Data
public class DeleteDetails {
    private TargetTable targetTable;
    private FromClause usingClause;
    private List<JoinCondition> joins = new ArrayList<>();
    private WhereClause where;
    private OrderByClause orderBy;
    private LimitClause limit;
}
