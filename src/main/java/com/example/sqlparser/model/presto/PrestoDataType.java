package com.example.sqlparser.model.presto;

/**
 * Presto 特有的数据类型
 */
public enum PrestoDataType {
    // 基础类型
    BOOLEAN,
    TINYINT,
    SMALLINT,
    INTEGER,
    BIGINT,
    REAL,
    DOUBLE,
    DECIMAL,
    
    // 字符串类型
    VARCHAR,
    CHAR,
    VARBINARY,
    
    // 时间类型
    DATE,
    TIME,
    TIME_WITH_TIME_ZONE,
    TIMESTAMP,
    TIMESTAMP_WITH_TIME_ZONE,
    INTERVAL,
    
    // 复杂类型
    ARRAY,
    MAP,
    ROW,
    JSON,
    IPADDRESS,
    UUID,
    
    // HyperLogLog
    HYPERLOGLOG,
    P4HYPERLOGLOG,
    
    // SetDigest
    SETDIGEST,
    
    // QuantileDigest
    QDIGEST,
    TDIGEST
}
