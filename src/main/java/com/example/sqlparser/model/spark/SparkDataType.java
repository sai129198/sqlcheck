package com.example.sqlparser.model.spark;

/**
 * Spark 特有的数据类型
 */
public enum SparkDataType {
    // 基础类型
    BOOLEAN,
    TINYINT,
    SMALLINT,
    INT,
    BIGINT,
    FLOAT,
    DOUBLE,
    DECIMAL,
    STRING,
    
    // 二进制类型
    BINARY,
    
    // 日期时间类型
    DATE,
    TIMESTAMP,
    INTERVAL,
    
    // 复杂类型
    ARRAY,
    MAP,
    STRUCT,
    
    // 特殊类型
    VOID,
    VARIANT
}
