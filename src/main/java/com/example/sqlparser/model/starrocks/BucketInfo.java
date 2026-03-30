package com.example.sqlparser.model.starrocks;

import java.util.List;

/**
 * 分桶信息
 */
public class BucketInfo {
    private List<String> bucketColumns;
    private int bucketCount;
    private boolean autoBucket;

    public List<String> getBucketColumns() {
        return bucketColumns;
    }

    public void setBucketColumns(List<String> bucketColumns) {
        this.bucketColumns = bucketColumns;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(int bucketCount) {
        this.bucketCount = bucketCount;
    }

    public boolean isAutoBucket() {
        return autoBucket;
    }

    public void setAutoBucket(boolean autoBucket) {
        this.autoBucket = autoBucket;
    }
}
