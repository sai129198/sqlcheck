package com.example.sqlparser.model.presto;

import lombok.Data;
import java.util.List;

/**
 * Presto 函数调用
 */
@Data
public class PrestoFunction {
    
    private String name;
    private List<String> arguments;
    private String returnType;
    
    // Lambda 表达式
    private boolean lambda;
    private List<String> lambdaArguments;
    private String lambdaExpression;
    
    // 特殊函数类型
    private FunctionType type;
    
    public enum FunctionType {
        REGULAR,
        AGGREGATE,
        WINDOW,
        LAMBDA,
        CAST,
        TRY_CAST,
        NULLIF,
        COALESCE,
        IF,
        SWITCH,
        FORMAT
    }
}
