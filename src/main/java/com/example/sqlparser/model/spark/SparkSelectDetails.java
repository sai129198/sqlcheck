package com.example.sqlparser.model.spark;

import com.example.sqlparser.model.SelectDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Spark 特有的 SELECT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SparkSelectDetails extends SelectDetails {
    
    // LATERAL VIEW
    private List<LateralView> lateralViews;
    
    // DISTRIBUTE BY / CLUSTER BY
    private List<String> distributeBy;
    private List<String> clusterBy;
    private List<String> sortBy;
    
    // Hints
    private List<Hint> hints;
    
    // TRANSFORM (使用脚本转换)
    private TransformClause transform;
    
    // PIVOT / UNPIVOT
    private PivotClause pivot;
    private UnpivotClause unpivot;
    
    // SAMPLE
    private Sample sample;
    
    @Data
    public static class LateralView {
        private String expression;
        private String tableAlias;
        private List<String> columnAliases;
        private boolean outer;
    }
    
    @Data
    public static class Hint {
        private String name;
        private List<String> parameters;
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
    public static class PivotClause {
        private String aggregateFunction;
        private String pivotColumn;
        private List<String> pivotValues;
    }
    
    @Data
    public static class UnpivotClause {
        private List<String> columns;
        private String nameColumn;
        private String valueColumn;
    }
    
    @Data
    public static class Sample {
        private double percentage;
        private int numRows;
        private boolean withReplacement;
        private long seed;
    }
}
