package com.example.sqlparser.model.hive;

import com.example.sqlparser.model.InsertDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Hive 特有的 INSERT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HiveInsertDetails extends InsertDetails {
    
    // INSERT OVERWRITE
    private boolean overwrite;
    
    // INTO / TABLE
    private boolean tableKeyword;
    
    // 分区指定
    private List<PartitionSpec> partitionSpecs;
    private boolean dynamicPartition;
    
    // 从本地文件系统加载
    private boolean local;
    
    // 文件路径 (LOAD DATA)
    private String filePath;
    
    @Data
    public static class PartitionSpec {
        private String column;
        private String value;
    }
}
