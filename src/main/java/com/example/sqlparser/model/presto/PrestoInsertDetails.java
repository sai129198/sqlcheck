package com.example.sqlparser.model.presto;

import com.example.sqlparser.model.InsertDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Presto 特有的 INSERT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrestoInsertDetails extends InsertDetails {
    
    // INSERT OVERWRITE
    private boolean overwrite;
    
    // INSERT IF NOT EXISTS
    private boolean ifNotExists;
    
    // 分区列值
    private List<PartitionValue> partitionValues;
    
    // 目标列位置
    private String columnAliases;
    
    @Data
    public static class PartitionValue {
        private String column;
        private String value;
    }
}
