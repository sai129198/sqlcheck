package com.example.sqlparser.model.spark;

import lombok.Data;
import java.util.List;

/**
 * Spark CREATE TABLE 语句
 */
@Data
public class SparkCreateTable {
    
    private boolean temporary;
    private boolean globalTemporary;
    private boolean ifNotExists;
    private boolean orReplace;
    
    private String tableName;
    private String schema;
    
    // 列定义
    private List<SparkColumnDef> columns;
    
    // 使用 USING 子句指定格式
    private String using;
    
    // 表格式 (USING 的替代语法)
    private String format;
    
    // 选项
    private List<Option> options;
    
    // 分区列
    private List<String> partitionedBy;
    
    // 分桶
    private List<String> clusteredBy;
    private Integer numBuckets;
    private List<String> sortedBy;
    
    // 位置
    private String location;
    
    // 注释
    private String comment;
    
    // 表属性
    private List<Property> properties;
    
    // TBLPROPERTIES
    private List<TblProperty> tblProperties;
    
    // AS SELECT
    private String asSelect;
    
    @Data
    public static class Option {
        private String key;
        private String value;
    }
    
    @Data
    public static class Property {
        private String key;
        private String value;
    }
    
    @Data
    public static class TblProperty {
        private String key;
        private String value;
    }
}
