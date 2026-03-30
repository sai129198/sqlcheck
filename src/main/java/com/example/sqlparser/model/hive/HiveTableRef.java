package com.example.sqlparser.model.hive;

import com.example.sqlparser.model.TableRef;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Hive 表引用
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HiveTableRef extends TableRef {
    
    // 是否为外部表
    private boolean external;
    
    // 是否为临时表
    private boolean temporary;
    
    // 存储格式 (TEXTFILE, ORC, PARQUET, AVRO, SEQUENCEFILE, RCFILE)
    private String storedAs;
    
    // 输入/输出格式
    private String inputFormat;
    private String outputFormat;
    
    // 行格式
    private String rowFormat;
    private String fieldDelim;
    private String collectionDelim;
    private String mapKeyDelim;
    private String lineDelim;
    
    // 存储位置
    private String location;
    
    // 分区列
    private List<String> partitionedBy;
    
    // 分桶
    private List<String> clusteredBy;
    private int numBuckets;
    private List<String> sortedBy;
    
    // 表属性
    private List<TableProperty> properties;
    
    // 表注释
    private String comment;
    
    @Data
    public static class TableProperty {
        private String key;
        private String value;
    }
}
