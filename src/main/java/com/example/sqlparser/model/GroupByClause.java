package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * GROUP BY 子句
 */
@Data
public class GroupByClause {
    private List<ColumnRef> columns = new ArrayList<>();
}
