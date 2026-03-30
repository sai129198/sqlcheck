package com.example.sqlparser.model.starrocks;

/**
 * 分区列
 */
public class PartitionColumn {
    private String name;
    private String dataType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
