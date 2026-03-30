package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 字段引用
 */
@Data
public class ColumnRef {
    private String name;
    private String alias;
    private TableRef sourceTable;
    private LineageInfo lineage;
}
