package com.example.sqlparser.model.impala;

import java.util.*;

/**
 * Impala 元数据操作语句
 */
public class ImpalaMetadataStmt {
    
    private MetadataStmtType stmtType;
    private String target;
    private String database;
    private String tableName;
    private boolean ifExists;
    private boolean ifNotExists;
    private boolean reload;
    private boolean incremental;
    
    // COMPUTE STATS 特有
    private List<String> columns = new ArrayList<>();
    private boolean forAllColumns;
    
    // REFRESH / INVALIDATE 特有
    private List<String> partitionSpecs = new ArrayList<>();
    
    // DESCRIBE 特有
    private boolean formatted;
    private boolean extended;
    
    // SHOW 特有
    private ShowType showType;
    private String pattern;
    private String inDatabase;
    
    // EXPLAIN 特有
    private ImpalaSelectDetails.ExplainLevel explainLevel;
    
    public enum MetadataStmtType {
        COMPUTE_STATS,
        DROP_STATS,
        REFRESH,
        REFRESH_FUNCTIONS,
        INVALIDATE_METADATA,
        DESCRIBE,
        DESCRIBE_DATABASE,
        SHOW_TABLES,
        SHOW_DATABASES,
        SHOW_FUNCTIONS,
        SHOW_CREATE_TABLE,
        SHOW_TABLE_STATS,
        SHOW_COLUMN_STATS,
        SHOW_PARTITIONS,
        SHOW_FILES,
        SHOW_ROLES,
        SHOW_GRANT,
        EXPLAIN
    }
    
    public enum ShowType {
        TABLES,
        DATABASES,
        FUNCTIONS,
        CREATE_TABLE,
        TABLE_STATS,
        COLUMN_STATS,
        PARTITIONS,
        FILES,
        ROLES,
        GRANT,
        CURRENT_ROLES,
        ROLE_GRANT
    }
    
    // Getters and Setters
    public MetadataStmtType getStmtType() {
        return stmtType;
    }
    
    public void setStmtType(MetadataStmtType stmtType) {
        this.stmtType = stmtType;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
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
    
    public boolean isReload() {
        return reload;
    }
    
    public void setReload(boolean reload) {
        this.reload = reload;
    }
    
    public boolean isIncremental() {
        return incremental;
    }
    
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    
    public boolean isForAllColumns() {
        return forAllColumns;
    }
    
    public void setForAllColumns(boolean forAllColumns) {
        this.forAllColumns = forAllColumns;
    }
    
    public List<String> getPartitionSpecs() {
        return partitionSpecs;
    }
    
    public void setPartitionSpecs(List<String> partitionSpecs) {
        this.partitionSpecs = partitionSpecs;
    }
    
    public boolean isFormatted() {
        return formatted;
    }
    
    public void setFormatted(boolean formatted) {
        this.formatted = formatted;
    }
    
    public boolean isExtended() {
        return extended;
    }
    
    public void setExtended(boolean extended) {
        this.extended = extended;
    }
    
    public ShowType getShowType() {
        return showType;
    }
    
    public void setShowType(ShowType showType) {
        this.showType = showType;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    public String getInDatabase() {
        return inDatabase;
    }
    
    public void setInDatabase(String inDatabase) {
        this.inDatabase = inDatabase;
    }
    
    public ImpalaSelectDetails.ExplainLevel getExplainLevel() {
        return explainLevel;
    }
    
    public void setExplainLevel(ImpalaSelectDetails.ExplainLevel explainLevel) {
        this.explainLevel = explainLevel;
    }
    
    @Override
    public String toString() {
        return "ImpalaMetadataStmt{" +
                "stmtType=" + stmtType +
                ", target='" + target + '\'' +
                ", database='" + database + '\'' +
                ", tableName='" + tableName + '\'' +
                '}';
    }
}
