package com.example.sqlparser.model.starrocks;

import com.example.sqlparser.model.TableRef;

/**
 * StarRocks 表引用（扩展支持分桶、分区等特性）
 */
public class StarRocksTableRef extends TableRef {
    
    // StarRocks 特有的表模型
    private StarRocksTableType tableType;
    
    // 分桶信息
    private BucketInfo bucketInfo;
    
    // 分区信息
    private PartitionInfo partitionInfo;
    
    // 物化视图相关
    private boolean materializedView;
    private String mvRefreshType;
    
    // 外部表信息
    private boolean externalTable;
    private String externalResource;

    public StarRocksTableType getTableType() {
        return tableType;
    }

    public void setTableType(StarRocksTableType tableType) {
        this.tableType = tableType;
    }

    public BucketInfo getBucketInfo() {
        return bucketInfo;
    }

    public void setBucketInfo(BucketInfo bucketInfo) {
        this.bucketInfo = bucketInfo;
    }

    public PartitionInfo getPartitionInfo() {
        return partitionInfo;
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    public boolean isMaterializedView() {
        return materializedView;
    }

    public void setMaterializedView(boolean materializedView) {
        this.materializedView = materializedView;
    }

    public String getMvRefreshType() {
        return mvRefreshType;
    }

    public void setMvRefreshType(String mvRefreshType) {
        this.mvRefreshType = mvRefreshType;
    }

    public boolean isExternalTable() {
        return externalTable;
    }

    public void setExternalTable(boolean externalTable) {
        this.externalTable = externalTable;
    }

    public String getExternalResource() {
        return externalResource;
    }

    public void setExternalResource(String externalResource) {
        this.externalResource = externalResource;
    }
}
