package com.example.sqlparser.model.impala;

import com.example.sqlparser.model.*;
import java.util.*;

/**
 * Impala INSERT 特有详情
 */
public class ImpalaInsertDetails extends InsertDetails {
    
    private boolean overwrite;
    private boolean into;
    private List<PartitionSpec> partitionSpecs = new ArrayList<>();
    private boolean shuffle;
    private boolean noshuffle;
    private boolean cached;
    private Integer cacheReplication;
    private boolean ifNotExists;
    
    // 公共类定义
    public static class PartitionSpec {
        private String column;
        private String value;
        
        public String getColumn() {
            return column;
        }
        
        public void setColumn(String column) {
            this.column = column;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "PartitionSpec{" +
                    "column='" + column + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
    
    // Getters and Setters
    public boolean isOverwrite() {
        return overwrite;
    }
    
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
    
    public boolean isInto() {
        return into;
    }
    
    public void setInto(boolean into) {
        this.into = into;
    }
    
    public List<PartitionSpec> getPartitionSpecs() {
        return partitionSpecs;
    }
    
    public void setPartitionSpecs(List<PartitionSpec> partitionSpecs) {
        this.partitionSpecs = partitionSpecs;
    }
    
    public boolean isShuffle() {
        return shuffle;
    }
    
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }
    
    public boolean isNoshuffle() {
        return noshuffle;
    }
    
    public void setNoshuffle(boolean noshuffle) {
        this.noshuffle = noshuffle;
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
    
    public boolean isIfNotExists() {
        return ifNotExists;
    }
    
    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }
    
    @Override
    public String toString() {
        return "ImpalaInsertDetails{" +
                "overwrite=" + overwrite +
                ", into=" + into +
                ", partitionSpecs=" + partitionSpecs.size() +
                ", shuffle=" + shuffle +
                ", cached=" + cached +
                '}';
    }
}
