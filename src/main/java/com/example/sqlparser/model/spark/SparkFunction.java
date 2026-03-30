package com.example.sqlparser.model.spark;

import lombok.Data;

/**
 * Spark 函数调用
 */
@Data
public class SparkFunction {
    
    private String name;
    private boolean distinct;
    private boolean ignoreNulls;
    
    // 特殊函数类型
    private FunctionType type;
    
    // Lambda 表达式
    private boolean lambda;
    private String lambdaExpression;
    
    public enum FunctionType {
        REGULAR,
        AGGREGATE,
        WINDOW,
        LAMBDA,
        BUILTIN,
        UDF,
        UDAF
    }
}
