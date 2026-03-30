package com.example.sqlparser.model.spark;

import com.example.sqlparser.model.TableRef;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Spark 表引用
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SparkTableRef extends TableRef {
    
    // 是否为临时表
    private boolean temporary;
    
    // 是否为全局临时表
    private boolean globalTemporary;
    
    // 表格式 (PARQUET, ORC, JSON, CSV, DELTA, ICEBERG, HUDI)
    private String format;
    
    // 存储路径
    private String location;
    
    // 分区信息
    private List<String> partitionedBy;
    
    // 分桶信息
    private List<String> clusteredBy;
    private int numBuckets;
    
    // 排序信息
    private List<String> sortedBy;
    
    // 表属性
    private List<TableProperty> properties;
    
    // 表注释
    private String comment;
    
    // 是否使用 USING 子句
    private String using;
    
    @Data
    public static class TableProperty {
        private String key;
        private String value;
    }
}
