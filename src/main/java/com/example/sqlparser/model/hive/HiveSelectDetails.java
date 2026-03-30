package com.example.sqlparser.model.hive;

import com.example.sqlparser.model.SelectDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Hive 特有的 SELECT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HiveSelectDetails extends SelectDetails {
    
    // LATERAL VIEW
    private List<LateralView> lateralViews;
    
    // DISTRIBUTE BY / CLUSTER BY / SORT BY
    private List<String> distributeBy;
    private List<String> clusterBy;
    private List<String> sortBy;
    
    // TRANSFORM (使用脚本转换)
    private TransformClause transform;
    
    // MAP / REDUCE (Hive 旧语法)
    private MapReduceClause mapReduce;
    
    // 窗口函数
    private List<WindowFunction> windowFunctions;
    
    // 抽样
    private Sample sample;
    
    // 虚拟列
    private boolean includeVirtualColumns;
    
    @Data
    public static class LateralView {
        private String expression;
        private String tableAlias;
        private List<String> columnAliases;
        private boolean outer;
    }
    
    @Data
    public static class TransformClause {
        private List<String> columns;
        private String script;
        private String rowFormat;
        private String recordWriter;
        private String recordReader;
    }
    
    @Data
    public static class MapReduceClause {
        private String input;
        private String output;
        private String mapper;
        private String reducer;
    }
    
    @Data
    public static class WindowFunction {
        private String function;
        private String partitionBy;
        private String orderBy;
        private String windowFrame;
    }
    
    @Data
    public static class Sample {
        private double percentage;
        private int numRows;
        private boolean bucketSample;
        private int bucketNumber;
        private int totalBuckets;
    }
}
