package com.example.sqlparser.model.presto;

import lombok.Data;
import java.util.List;

/**
 * Presto CREATE TABLE 语句
 */
@Data
public class PrestoCreateTable {
    
    private boolean ifNotExists;
    private String tableName;
    private String schema;
    private String catalog;
    
    // 列定义
    private List<PrestoColumnDef> columns;
    
    // 表注释
    private String comment;
    
    // 表属性
    private List<Property> properties;
    
    // 是否包含数据
    private boolean withData;
    
    // AS SELECT 语句
    private String asSelect;
    
    // 分区列
    private List<String> partitionedBy;
    
    @Data
    public static class Property {
        private String key;
        private String value;
    }
}
