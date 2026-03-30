package com.example.sqlparser.model.clickhouse;

import lombok.Data;

/**
 * ClickHouse 列定义
 */
@Data
public class ClickHouseColumnDef {
    private String name;
    private String dataType;
    private String comment;
    
    // 默认值
    private String defaultValue;
    private String defaultType;  // DEFAULT, MATERIALIZED, ALIAS
    
    // 编解码器
    private String codec;
    
    // TTL
    private String ttl;
    
    // 是否可为空
    private boolean nullable;
}
