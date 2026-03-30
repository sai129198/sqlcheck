package com.example.sqlparser.model.starrocks;

import java.util.List;

/**
 * 分区信息
 */
public class PartitionInfo {
    private PartitionType type;
    private List<PartitionColumn> partitionColumns;
    private List<PartitionDefinition> partitions;

    public PartitionType getType() {
        return type;
    }

    public void setType(PartitionType type) {
        this.type = type;
    }

    public List<PartitionColumn> getPartitionColumns() {
        return partitionColumns;
    }

    public void setPartitionColumns(List<PartitionColumn> partitionColumns) {
        this.partitionColumns = partitionColumns;
    }

    public List<PartitionDefinition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionDefinition> partitions) {
        this.partitions = partitions;
    }
}
