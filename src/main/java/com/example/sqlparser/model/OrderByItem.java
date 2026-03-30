package com.example.sqlparser.model;

import lombok.Data;

/**
 * ORDER BY 项
 */
@Data
public class OrderByItem {
    private ColumnRef column;
    private boolean ascending = true;
}
