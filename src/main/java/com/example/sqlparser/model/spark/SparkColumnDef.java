package com.example.sqlparser.model.spark;

import lombok.Data;

/**
 * Spark 列定义
 */
@Data
public class SparkColumnDef {
    private String name;
    private String dataType;
    private boolean nullable;
    private String comment;
    private String defaultValue;
    
    // 生成列
    private boolean generated;
    private String generationExpression;
    
    // 约束
    private String constraints;
}
