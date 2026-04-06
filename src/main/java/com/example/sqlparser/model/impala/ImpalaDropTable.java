package com.example.sqlparser.model.impala;

import java.util.*;

/**
 * Impala DROP TABLE 语句
 */
public class ImpalaDropTable {
    
    private String database;
    private String tableName;
    private boolean ifExists;
    private boolean purge;
    
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
    
    public boolean isIfExists() {
        return ifExists;
    }
    
    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }
    
    public boolean isPurge() {
        return purge;
    }
    
    public void setPurge(boolean purge) {
        this.purge = purge;
    }
    
    @Override
    public String toString() {
        return "ImpalaDropTable{" +
                "database='" + database + '\'' +
                ", tableName='" + tableName + '\'' +
                ", ifExists=" + ifExists +
                ", purge=" + purge +
                '}';
    }
}
