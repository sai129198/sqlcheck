package com.example.sqlparser.model.impala;

import com.example.sqlparser.model.*;
import java.util.*;

/**
 * Impala ALTER TABLE 语句
 */
public class ImpalaAlterTable {
    
    private String database;
    private String tableName;
    private AlterType alterType;
    
    // ADD COLUMNS
    private List<ImpalaColumnDef> addColumns = new ArrayList<>();
    
    // REPLACE COLUMNS
    private List<ImpalaColumnDef> replaceColumns = new ArrayList<>();
    
    // DROP COLUMN
    private String dropColumn;
    
    // CHANGE COLUMN
    private String oldColumnName;
    private ImpalaColumnDef newColumnDef;
    
    // RENAME
    private String newTableName;
    
    // SET TBLPROPERTIES
    private Map<String, String> setProperties = new HashMap<>();
    
    // UNSET TBLPROPERTIES
    private List<String> unsetProperties = new ArrayList<>();
    
    // SET FILEFORMAT
    private ImpalaFileFormat fileFormat;
    
    // SET LOCATION
    private String newLocation;
    
    // SET SERDEPROPERTIES
    private Map<String, String> serdeProperties = new HashMap<>();
    
    // PARTITION 相关
    private String partitionSpec;
    private boolean ifExists;
    private boolean ifNotExists;
    private boolean purge;
    
    // RECOVER PARTITIONS
    private boolean recoverPartitions;
    
    // COMPUTE STATS
    private boolean computeStats;
    private List<String> computeStatsColumns = new ArrayList<>();
    
    // DROP STATS
    private boolean dropStats;
    
    public enum AlterType {
        ADD_COLUMNS,
        REPLACE_COLUMNS,
        DROP_COLUMN,
        CHANGE_COLUMN,
        RENAME,
        SET_TBLPROPERTIES,
        UNSET_TBLPROPERTIES,
        SET_FILEFORMAT,
        SET_LOCATION,
        SET_SERDEPROPERTIES,
        ADD_PARTITION,
        DROP_PARTITION,
        RECOVER_PARTITIONS,
        COMPUTE_STATS,
        DROP_STATS,
        SET_CACHED,
        SET_UNCACHED
    }
    
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
    
    public AlterType getAlterType() {
        return alterType;
    }
    
    public void setAlterType(AlterType alterType) {
        this.alterType = alterType;
    }
    
    public List<ImpalaColumnDef> getAddColumns() {
        return addColumns;
    }
    
    public void setAddColumns(List<ImpalaColumnDef> addColumns) {
        this.addColumns = addColumns;
    }
    
    public List<ImpalaColumnDef> getReplaceColumns() {
        return replaceColumns;
    }
    
    public void setReplaceColumns(List<ImpalaColumnDef> replaceColumns) {
        this.replaceColumns = replaceColumns;
    }
    
    public String getDropColumn() {
        return dropColumn;
    }
    
    public void setDropColumn(String dropColumn) {
        this.dropColumn = dropColumn;
    }
    
    public String getOldColumnName() {
        return oldColumnName;
    }
    
    public void setOldColumnName(String oldColumnName) {
        this.oldColumnName = oldColumnName;
    }
    
    public ImpalaColumnDef getNewColumnDef() {
        return newColumnDef;
    }
    
    public void setNewColumnDef(ImpalaColumnDef newColumnDef) {
        this.newColumnDef = newColumnDef;
    }
    
    public String getNewTableName() {
        return newTableName;
    }
    
    public void setNewTableName(String newTableName) {
        this.newTableName = newTableName;
    }
    
    public Map<String, String> getSetProperties() {
        return setProperties;
    }
    
    public void setSetProperties(Map<String, String> setProperties) {
        this.setProperties = setProperties;
    }
    
    public List<String> getUnsetProperties() {
        return unsetProperties;
    }
    
    public void setUnsetProperties(List<String> unsetProperties) {
        this.unsetProperties = unsetProperties;
    }
    
    public ImpalaFileFormat getFileFormat() {
        return fileFormat;
    }
    
    public void setFileFormat(ImpalaFileFormat fileFormat) {
        this.fileFormat = fileFormat;
    }
    
    public String getNewLocation() {
        return newLocation;
    }
    
    public void setNewLocation(String newLocation) {
        this.newLocation = newLocation;
    }
    
    public Map<String, String> getSerdeProperties() {
        return serdeProperties;
    }
    
    public void setSerdeProperties(Map<String, String> serdeProperties) {
        this.serdeProperties = serdeProperties;
    }
    
    public String getPartitionSpec() {
        return partitionSpec;
    }
    
    public void setPartitionSpec(String partitionSpec) {
        this.partitionSpec = partitionSpec;
    }
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }
    
    public boolean isIfNotExists() {
        return ifNotExists;
    }
    
    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }
    
    public boolean isPurge() {
        return purge;
    }
    
    public void setPurge(boolean purge) {
        this.purge = purge;
    }
    
    public boolean isRecoverPartitions() {
        return recoverPartitions;
    }
    
    public void setRecoverPartitions(boolean recoverPartitions) {
        this.recoverPartitions = recoverPartitions;
    }
    
    public boolean isComputeStats() {
        return computeStats;
    }
    
    public void setComputeStats(boolean computeStats) {
        this.computeStats = computeStats;
    }
    
    public List<String> getComputeStatsColumns() {
        return computeStatsColumns;
    }
    
    public void setComputeStatsColumns(List<String> computeStatsColumns) {
        this.computeStatsColumns = computeStatsColumns;
    }
    
    public boolean isDropStats() {
        return dropStats;
    }
    
    public void setDropStats(boolean dropStats) {
        this.dropStats = dropStats;
    }
    
    @Override
    public String toString() {
        return "ImpalaAlterTable{" +
                "database='" + database + '\'' +
                ", tableName='" + tableName + '\'' +
                ", alterType=" + alterType +
                '}';
    }
}
