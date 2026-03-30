package com.example.sqlparser.model.clickhouse;

import com.example.sqlparser.model.SelectDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * ClickHouse 特有的 SELECT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClickHouseSelectDetails extends SelectDetails {
    
    // ARRAY JOIN
    private List<ArrayJoin> arrayJoins;
    
    // SAMPLE 采样
    private Sample sample;
    
    // FINAL (用于 CollapsingMergeTree 等)
    private boolean finalModifier;
    
    // PREWHERE
    private String prewhere;
    
    // LIMIT n, m 语法 (ClickHouse 特有)
    private Long limitOffset;
    private Long limitCount;
    private boolean withTies;
    
    // GROUP BY WITH ROLLUP / CUBE / TOTALS
    private boolean withRollup;
    private boolean withCube;
    private boolean withTotals;
    
    // FORMAT 输出格式
    private String format;
    
    // SETTINGS 查询设置
    private List<Setting> settings;
    
    @Data
    public static class ArrayJoin {
        private String arrayExpression;
        private String alias;
        private boolean left;  // LEFT ARRAY JOIN
    }
    
    @Data
    public static class Sample {
        private double ratio;  // SAMPLE 0.1
        private long offset;   // SAMPLE 0.1 OFFSET 0.5
        private long n;        // SAMPLE 1000000
        private long m;        // SAMPLE 1000000 OFFSET 500000
    }
    
    @Data
    public static class Setting {
        private String key;
        private String value;
    }
}
