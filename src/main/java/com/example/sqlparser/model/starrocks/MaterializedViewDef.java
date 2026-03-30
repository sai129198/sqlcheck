package com.example.sqlparser.model.starrocks;

import java.util.List;

/**
 * 物化视图定义
 */
public class MaterializedViewDef {
    private String name;
    private List<String> columns;
    private String query;
    private String refreshType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRefreshType() {
        return refreshType;
    }

    public void setRefreshType(String refreshType) {
        this.refreshType = refreshType;
    }
}
