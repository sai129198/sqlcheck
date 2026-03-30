package com.example.sqlparser.model.presto;

import lombok.Data;
import java.util.List;

/**
 * Presto EXPLAIN 语句
 */
@Data
public class PrestoExplain {
    
    private ExplainType type;
    private String statement;
    private List<String> options;
    
    public enum ExplainType {
        LOGICAL,
        DISTRIBUTED,
        VALIDATE,
        IO
    }
}
