package com.example.sqlparser.model.spark;

import lombok.Data;

/**
 * Spark EXPLAIN 语句
 */
@Data
public class SparkExplain {
    
    private ExplainType type;
    private String statement;
    private boolean extended;
    private boolean codegen;
    private boolean cost;
    private boolean formatted;
    
    public enum ExplainType {
        SIMPLE,
        EXTENDED,
        CODEGEN,
        COST,
        FORMATTED
    }
}
