package com.example.sqlparser.model.clickhouse;

import lombok.Data;
import java.util.List;

/**
 * ClickHouse CREATE TABLE 语句
 */
@Data
public class ClickHouseCreateTable {
    
    private boolean temporary;
    private boolean ifNotExists;
    private boolean orReplace;
    
    private String tableName;
    private String database;
    
    // 列定义
    private List<ClickHouseColumnDef> columns;
    
    // 表引擎 (MergeTree, ReplacingMergeTree, SummingMergeTree 等)
    private String engine;
    
    // 引擎参数
    private List<String> engineParams;
    
    // 分区键
    private String partitionBy;
    
    // 排序键
    private String orderBy;
    
    // 主键
    private String primaryKey;
    
    // 采样键
    private String sampleBy;
    
    // TTL
    private String ttl;
    
    // 表设置
    private List<Setting> settings;
    
    // 注释
    private String comment;
    
    // AS SELECT
    private String asSelect;
    
    @Data
    public static class Setting {
        private String key;
        private String value;
    }
}
