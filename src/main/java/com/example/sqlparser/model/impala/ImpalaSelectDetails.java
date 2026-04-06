package com.example.sqlparser.model.impala;

import com.example.sqlparser.model.*;
import java.util.*;

/**
 * Impala SELECT 特有详情
 */
public class ImpalaSelectDetails extends SelectDetails {
    
    // Impala 特有查询选项
    private boolean straightJoin;
    private boolean shuffle;
    private boolean noshuffle;
    private boolean broadcast;
    private boolean nobroadcast;
    private Integer replicaPreference;
    private String randomReplica;
    private boolean cacheTable;
    private Integer cacheReplication;
    
    // Hints
    private List<Hint> hints = new ArrayList<>();
    
    // 统计信息
    private ExplainLevel explainLevel;
    private boolean computeStats;
    private List<String> computeStatsColumns = new ArrayList<>();
    
    // 公共类定义
    public static class Hint {
        private String name;
        private List<String> args = new ArrayList<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<String> getArgs() {
            return args;
        }
        
        public void setArgs(List<String> args) {
            this.args = args;
        }
        
        @Override
        public String toString() {
            return "Hint{" +
                    "name='" + name + '\'' +
                    ", args=" + args +
                    '}';
        }
    }
    
    public enum ExplainLevel {
        MINIMAL,
        STANDARD,
        EXTENDED,
        VERBOSE
    }
    
    // Getters and Setters
    public boolean isStraightJoin() {
        return straightJoin;
    }
    
    public void setStraightJoin(boolean straightJoin) {
        this.straightJoin = straightJoin;
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
    
    public boolean isBroadcast() {
        return broadcast;
    }
    
    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }
    
    public boolean isNobroadcast() {
        return nobroadcast;
    }
    
    public void setNobroadcast(boolean nobroadcast) {
        this.nobroadcast = nobroadcast;
    }
    
    public Integer getReplicaPreference() {
        return replicaPreference;
    }
    
    public void setReplicaPreference(Integer replicaPreference) {
        this.replicaPreference = replicaPreference;
    }
    
    public String getRandomReplica() {
        return randomReplica;
    }
    
    public void setRandomReplica(String randomReplica) {
        this.randomReplica = randomReplica;
    }
    
    public boolean isCacheTable() {
        return cacheTable;
    }
    
    public void setCacheTable(boolean cacheTable) {
        this.cacheTable = cacheTable;
    }
    
    public Integer getCacheReplication() {
        return cacheReplication;
    }
    
    public void setCacheReplication(Integer cacheReplication) {
        this.cacheReplication = cacheReplication;
    }
    
    public List<Hint> getHints() {
        return hints;
    }
    
    public void setHints(List<Hint> hints) {
        this.hints = hints;
    }
    
    public ExplainLevel getExplainLevel() {
        return explainLevel;
    }
    
    public void setExplainLevel(ExplainLevel explainLevel) {
        this.explainLevel = explainLevel;
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
    
    @Override
    public String toString() {
        return "ImpalaSelectDetails{" +
                "straightJoin=" + straightJoin +
                ", shuffle=" + shuffle +
                ", noshuffle=" + noshuffle +
                ", hints=" + hints.size() +
                ", computeStats=" + computeStats +
                '}';
    }
}
