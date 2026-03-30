package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询块
 */
@Data
public class QueryBlock {
    private QueryBlockType type;
    
    // 简单 SELECT 的元素
    private SelectClause select;
    private FromClause from;
    private WhereClause where;
    private GroupByClause groupBy;
    private HavingClause having;
    private OrderByClause orderBy;
    private LimitClause limit;
    
    // 集合操作
    private QueryBlock leftQuery;
    private QueryBlock rightQuery;
    private SetOperationType setOpType;
    private boolean setOpAll;
    
    // 嵌套关系
    private QueryBlock parent;
    private List<SubQueryRef> containedSubQueries = new ArrayList<>();
}
