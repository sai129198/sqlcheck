package com.example.sqlparser.model.mysql;

/**
 * MySQL 特有的数据类型
 */
public enum MySQLDataType {
    // 整数类型
    TINYINT,
    SMALLINT,
    MEDIUMINT,
    INT,
    INTEGER,
    BIGINT,
    
    // 浮点类型
    FLOAT,
    DOUBLE,
    DECIMAL,
    NUMERIC,
    
    // 日期时间类型
    DATE,
    TIME,
    DATETIME,
    TIMESTAMP,
    YEAR,
    
    // 字符串类型
    CHAR,
    VARCHAR,
    TINYTEXT,
    TEXT,
    MEDIUMTEXT,
    LONGTEXT,
    
    // 二进制类型
    BINARY,
    VARBINARY,
    TINYBLOB,
    BLOB,
    MEDIUMBLOB,
    LONGBLOB,
    
    // JSON
    JSON,
    
    // 空间类型
    GEOMETRY,
    POINT,
    LINESTRING,
    POLYGON,
    
    // 枚举和集合
    ENUM,
    SET
}
