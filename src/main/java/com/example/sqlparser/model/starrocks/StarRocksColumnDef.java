package com.example.sqlparser.model.starrocks;

/**
 * StarRocks 列定义
 */
public class StarRocksColumnDef {
    private String name;
    private String dataType;
    private boolean nullable;
    private String defaultValue;
    private String comment;
    
    // 聚合类型（AGGREGATE KEY 表使用）
    private AggregateType aggregateType;
    
    // Bitmap 索引
    private boolean bitmapIndex;
    
    // Bloom filter
    private boolean bloomFilter;

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

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public AggregateType getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(AggregateType aggregateType) {
        this.aggregateType = aggregateType;
    }

    public boolean isBitmapIndex() {
        return bitmapIndex;
    }

    public void setBitmapIndex(boolean bitmapIndex) {
        this.bitmapIndex = bitmapIndex;
    }

    public boolean isBloomFilter() {
        return bloomFilter;
    }

    public void setBloomFilter(boolean bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    public enum AggregateType {
        SUM,
        MIN,
        MAX,
        REPLACE,
        REPLACE_IF_NOT_NULL,
        HLL_UNION,
        BITMAP_UNION,
        PERCENTILE_UNION
    }
}
