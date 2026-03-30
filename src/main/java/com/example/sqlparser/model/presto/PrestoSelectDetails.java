package com.example.sqlparser.model.presto;

import com.example.sqlparser.model.SelectDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Presto 特有的 SELECT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrestoSelectDetails extends SelectDetails {
    
    // WITH RECURSIVE 递归 CTE
    private boolean recursive;
    private List<RecursiveCte> recursiveCtes;
    
    // UNNEST 展开数组/MAP
    private List<UnnestClause> unnestClauses;
    
    // TABLESAMPLE 采样
    private TableSample tableSample;
    
    // MATCH_RECOGNIZE 模式匹配
    private MatchRecognize matchRecognize;
    
    // 窗口函数
    private List<WindowFunction> windowFunctions;
    
    // LIMIT ALL
    private boolean limitAll;
    
    // OFFSET
    private long offset;
    
    // FETCH FIRST
    private FetchFirst fetchFirst;
    
    @Data
    public static class RecursiveCte {
        private String name;
        private List<String> columns;
        private String anchorQuery;
        private String recursiveQuery;
    }
    
    @Data
    public static class UnnestClause {
        private List<String> arrays;
        private String alias;
        private boolean withOrdinality;
    }
    
    @Data
    public static class TableSample {
        private String type;  // BERNOULLI / SYSTEM
        private double percentage;
    }
    
    @Data
    public static class MatchRecognize {
        private String partitionBy;
        private String orderBy;
        private String measures;
        private String pattern;
        private String define;
    }
    
    @Data
    public static class WindowFunction {
        private String function;
        private String windowName;
        private String partitionBy;
        private String orderBy;
        private String frame;
    }
    
    @Data
    public static class FetchFirst {
        private long count;
        private boolean withTies;
    }
}
