package com.example.sqlparser.model.impala;

/**
 * Impala 数据类型枚举
 */
public enum ImpalaDataType {
    // 数值类型
    TINYINT,
    SMALLINT,
    INT,
    INTEGER,
    BIGINT,
    FLOAT,
    DOUBLE,
    REAL,
    DECIMAL,
    NUMERIC,
    
    // 字符串类型
    STRING,
    VARCHAR,
    CHAR,
    
    // 二进制类型
    BINARY,
    
    // 时间类型
    TIMESTAMP,
    DATE,
    
    // 布尔类型
    BOOLEAN,
    
    // 复杂类型
    ARRAY,
    MAP,
    STRUCT
}
