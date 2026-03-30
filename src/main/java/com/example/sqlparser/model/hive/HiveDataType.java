package com.example.sqlparser.model.hive;

/**
 * Hive 特有的数据类型
 */
public enum HiveDataType {
    // 基础类型
    TINYINT,
    SMALLINT,
    INT,
    BIGINT,
    BOOLEAN,
    FLOAT,
    DOUBLE,
    DECIMAL,
    STRING,
    
    // 日期时间类型
    TIMESTAMP,
    DATE,
    INTERVAL,
    
    // 二进制类型
    BINARY,
    
    // 复杂类型
    ARRAY,
    MAP,
    STRUCT,
    UNIONTYPE,
    
    // 其他类型
    VARCHAR,
    CHAR
}
