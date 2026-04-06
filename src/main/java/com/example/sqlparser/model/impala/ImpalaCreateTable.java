package com.example.sqlparser.model.impala;

import java.util.*;

/**
 * Impala CREATE TABLE 语句
 */
public class ImpalaCreateTable {
    private String database;
    private String tableName;
    private boolean external;
    private boolean ifNotExists;
    private List<ImpalaColumnDef> columns = new ArrayList<>();
    private List<ImpalaColumnDef> partitionColumns = new ArrayList<>();
    private ImpalaFileFormat storedAs;
    private String rowFormat;
    private String location;
    private String comment;
    private Map<String, String> tblProperties = new HashMap<>();
    private String asSelect;
    private String likeTable;
    private boolean cached;
    private Integer cacheReplication;
    private ImpalaCachingPolicy cachingPolicy;
    private String sortColumns;
    private Integer numBuckets;
    private List<String> clusteredBy = new ArrayList<>();
    private List<String> sortedBy = new ArrayList<>();
    private String fieldTerminator;
    private String lineTerminator;
    private String collectionItemsTerminated;
    private String mapKeysTerminated;
    private String escapedBy;
    private String nullDefinedAs;
    private String serdeClass;
    private Map<String, String> serdeProperties = new HashMap<>();
    private boolean kuduTable;
    private List<String> primaryKey = new ArrayList<>();
    private List<String> distributeBy = new ArrayList<>();
    private Integer distributeInto;
    private String partitionBy;
    private String rangePartition;
    
    // Getters and Setters
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public boolean isExternal() {
        return external;
    }
    
    public void setExternal(boolean external) {
        this.external = external;
    }
    
    public boolean isIfNotExists() {
        return ifNotExists;
    }
    
    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }
    
    public List<ImpalaColumnDef> getColumns() {
        return columns;
    }
    
    public void setColumns(List<ImpalaColumnDef> columns) {
        this.columns = columns;
    }
    
    public List<ImpalaColumnDef> getPartitionColumns() {
        return partitionColumns;
    }
    
    public void setPartitionColumns(List<ImpalaColumnDef> partitionColumns) {
        this.partitionColumns = partitionColumns;
    }
    
    public ImpalaFileFormat getStoredAs() {
        return storedAs;
    }
    
    public void setStoredAs(ImpalaFileFormat storedAs) {
        this.storedAs = storedAs;
    }
    
    public String getRowFormat() {
        return rowFormat;
    }
    
    public void setRowFormat(String rowFormat) {
        this.rowFormat = rowFormat;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public Map<String, String> getTblProperties() {
        return tblProperties;
    }
    
    public void setTblProperties(Map<String, String> tblProperties) {
        this.tblProperties = tblProperties;
    }
    
    public String getAsSelect() {
        return asSelect;
    }
    
    public void setAsSelect(String asSelect) {
        this.asSelect = asSelect;
    }
    
    public String getLikeTable() {
        return likeTable;
    }
    
    public void setLikeTable(String likeTable) {
        this.likeTable = likeTable;
    }
    
    public boolean isCached() {
        return cached;
    }
    
    public void setCached(boolean cached) {
        this.cached = cached;
    }
    
    public Integer getCacheReplication() {
        return cacheReplication;
    }
    
    public void setCacheReplication(Integer cacheReplication) {
        this.cacheReplication = cacheReplication;
    }
    
    public ImpalaCachingPolicy getCachingPolicy() {
        return cachingPolicy;
    }
    
    public void setCachingPolicy(ImpalaCachingPolicy cachingPolicy) {
        this.cachingPolicy = cachingPolicy;
    }
    
    public String getSortColumns() {
        return sortColumns;
    }
    
    public void setSortColumns(String sortColumns) {
        this.sortColumns = sortColumns;
    }
    
    public Integer getNumBuckets() {
        return numBuckets;
    }
    
    public void setNumBuckets(Integer numBuckets) {
        this.numBuckets = numBuckets;
    }
    
    public List<String> getClusteredBy() {
        return clusteredBy;
    }
    
    public void setClusteredBy(List<String> clusteredBy) {
        this.clusteredBy = clusteredBy;
    }
    
    public List<String> getSortedBy() {
        return sortedBy;
    }
    
    public void setSortedBy(List<String> sortedBy) {
        this.sortedBy = sortedBy;
    }
    
    public String getFieldTerminator() {
        return fieldTerminator;
    }
    
    public void setFieldTerminator(String fieldTerminator) {
        this.fieldTerminator = fieldTerminator;
    }
    
    public String getLineTerminator() {
        return lineTerminator;
    }
    
    public void setLineTerminator(String lineTerminator) {
        this.lineTerminator = lineTerminator;
    }
    
    public String getCollectionItemsTerminated() {
        return collectionItemsTerminated;
    }
    
    public void setCollectionItemsTerminated(String collectionItemsTerminated) {
        this.collectionItemsTerminated = collectionItemsTerminated;
    }
    
    public String getMapKeysTerminated() {
        return mapKeysTerminated;
    }
    
    public void setMapKeysTerminated(String mapKeysTerminated) {
        this.mapKeysTerminated = mapKeysTerminated;
    }
    
    public String getEscapedBy() {
        return escapedBy;
    }
    
    public void setEscapedBy(String escapedBy) {
        this.escapedBy = escapedBy;
    }
    
    public String getNullDefinedAs() {
        return nullDefinedAs;
    }
    
    public void setNullDefinedAs(String nullDefinedAs) {
        this.nullDefinedAs = nullDefinedAs;
    }
    
    public String getSerdeClass() {
        return serdeClass;
    }
    
    public void setSerdeClass(String serdeClass) {
        this.serdeClass = serdeClass;
    }
    
    public Map<String, String> getSerdeProperties() {
        return serdeProperties;
    }
    
    public void setSerdeProperties(Map<String, String> serdeProperties) {
        this.serdeProperties = serdeProperties;
    }
    
    public boolean isKuduTable() {
        return kuduTable;
    }
    
    public void setKuduTable(boolean kuduTable) {
        this.kuduTable = kuduTable;
    }
    
    public List<String> getPrimaryKey() {
        return primaryKey;
    }
    
    public void setPrimaryKey(List<String> primaryKey) {
        this.primaryKey = primaryKey;
    }
    
    public List<String> getDistributeBy() {
        return distributeBy;
    }
    
    public void setDistributeBy(List<String> distributeBy) {
        this.distributeBy = distributeBy;
    }
    
    public Integer getDistributeInto() {
        return distributeInto;
    }
    
    public void setDistributeInto(Integer distributeInto) {
        this.distributeInto = distributeInto;
    }
    
    public String getPartitionBy() {
        return partitionBy;
    }
    
    public void setPartitionBy(String partitionBy) {
        this.partitionBy = partitionBy;
    }
    
    public String getRangePartition() {
        return rangePartition;
    }
    
    public void setRangePartition(String rangePartition) {
        this.rangePartition = rangePartition;
    }
    
    @Override
    public String toString() {
        return "ImpalaCreateTable{" +
                "database='" + database + '\'' +
                ", tableName='" + tableName + '\'' +
                ", external=" + external +
                ", ifNotExists=" + ifNotExists +
                ", columns=" + columns.size() +
                ", partitionColumns=" + partitionColumns.size() +
                ", storedAs=" + storedAs +
                ", location='" + location + '\'' +
                ", comment='" + comment + '\'' +
                ", cached=" + cached +
                ", kuduTable=" + kuduTable +
                '}';
    }
}
