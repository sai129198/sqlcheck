package com.example.sqlparser.model.starrocks;

import java.util.ArrayList;
import java.util.List;

/**
 * StarRocks CREATE TABLE 语句
 */
public class StarRocksCreateTable {
    
    private boolean ifNotExists;
    private String tableName;
    private String database;
    
    // 列定义
    private List<StarRocksColumnDef> columns = new ArrayList<>();
    
    // 表模型类型
    private StarRocksTableType tableType;
    
    // 主键/排序键
    private List<String> primaryKeys;
    private List<String> sortKeys;
    private List<String> aggregateKeys;
    private List<String> duplicateKeys;
    
    // 分区和分桶
    private PartitionInfo partitionInfo;
    private BucketInfo bucketInfo;
    
    // 物化视图定义
    private List<MaterializedViewDef> materializedViews = new ArrayList<>();
    
    // 属性设置
    private List<Property> properties = new ArrayList<>();
    
    // 外部表
    private boolean external;
    private String engine;

    public boolean isIfNotExists() {
        return ifNotExists;
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public List<StarRocksColumnDef> getColumns() {
        return columns;
    }

    public void setColumns(List<StarRocksColumnDef> columns) {
        this.columns = columns;
    }

    public StarRocksTableType getTableType() {
        return tableType;
    }

    public void setTableType(StarRocksTableType tableType) {
        this.tableType = tableType;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<String> getSortKeys() {
        return sortKeys;
    }

    public void setSortKeys(List<String> sortKeys) {
        this.sortKeys = sortKeys;
    }

    public List<String> getAggregateKeys() {
        return aggregateKeys;
    }

    public void setAggregateKeys(List<String> aggregateKeys) {
        this.aggregateKeys = aggregateKeys;
    }

    public List<String> getDuplicateKeys() {
        return duplicateKeys;
    }

    public void setDuplicateKeys(List<String> duplicateKeys) {
        this.duplicateKeys = duplicateKeys;
    }

    public PartitionInfo getPartitionInfo() {
        return partitionInfo;
    }

    public void setPartitionInfo(PartitionInfo partitionInfo) {
        this.partitionInfo = partitionInfo;
    }

    public BucketInfo getBucketInfo() {
        return bucketInfo;
    }

    public void setBucketInfo(BucketInfo bucketInfo) {
        this.bucketInfo = bucketInfo;
    }

    public List<MaterializedViewDef> getMaterializedViews() {
        return materializedViews;
    }

    public void setMaterializedViews(List<MaterializedViewDef> materializedViews) {
        this.materializedViews = materializedViews;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * 属性类
     */
    public static class Property {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
