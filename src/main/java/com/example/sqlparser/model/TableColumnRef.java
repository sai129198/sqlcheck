package com.example.sqlparser.model;

import lombok.Data;

/**
 * 表字段引用
 */
@Data
public class TableColumnRef {
    private String tableName;
    private String columnName;
    private String schema;
    private TableRef tableRef;
}
