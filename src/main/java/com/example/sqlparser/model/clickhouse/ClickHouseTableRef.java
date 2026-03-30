package com.example.sqlparser.model.clickhouse;

import com.example.sqlparser.model.TableRef;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * ClickHouse 表引用
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClickHouseTableRef extends TableRef {
    
    // 数据库引擎
    private String engine;
    
    // 是否为分布式表
    private boolean distributed;
    private String cluster;
    
    // 是否为物化视图
    private boolean materializedView;
    
    // 是否为临时表
    private boolean temporary;
    
    // 表设置
    private List<TableSetting> settings;
    
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
    
    @Data
    public static class TableSetting {
        private String key;
        private String value;
    }
}
