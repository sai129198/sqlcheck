package com.example.sqlparser.model.hive;

import lombok.Data;

/**
 * Hive 函数调用
 */
@Data
public class HiveFunction {
    
    private String name;
    private boolean distinct;
    
    // 特殊函数类型
    private FunctionType type;
    
    // UDF/UDAF/UDTF
    private boolean userDefined;
    private String udfClass;
    
    // 窗口函数
    private boolean windowFunction;
    private String windowSpec;
    
    // 分析函数
    private boolean analyticFunction;
    
    public enum FunctionType {
        REGULAR,
        AGGREGATE,
        WINDOW,
        ANALYTIC,
        UDF,
        UDAF,
        UDTF,
        BUILTIN,
        CAST
    }
}
