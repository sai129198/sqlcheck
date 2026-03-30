package com.example.sqlparser.model.spark;

import com.example.sqlparser.model.InsertDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Spark 特有的 INSERT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SparkInsertDetails extends InsertDetails {
    
    // INSERT OVERWRITE
    private boolean overwrite;
    
    // INSERT INTO
    private boolean into;
    
    // TABLE 关键字 (INSERT INTO TABLE)
    private boolean tableKeyword;
    
    // 分区指定
    private List<PartitionSpec> partitionSpecs;
    
    // IF NOT EXISTS
    private boolean ifNotExists;
    
    // 动态分区
    private boolean dynamicPartition;
    
    @Data
    public static class PartitionSpec {
        private String column;
        private String value;
    }
}
