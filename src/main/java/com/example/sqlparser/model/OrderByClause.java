package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * ORDER BY 子句
 */
@Data
public class OrderByClause {
    private List<OrderByItem> items = new ArrayList<>();
}
