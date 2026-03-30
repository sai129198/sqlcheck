package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECT 子句
 */
@Data
public class SelectClause {
    private boolean distinct;
    private List<ColumnRef> columns = new ArrayList<>();
}
