package com.example.sqlparser.model.clickhouse;

/**
 * ClickHouse 特有的数据类型
 */
public enum ClickHouseDataType {
    // 整数类型
    Int8,
    Int16,
    Int32,
    Int64,
    Int128,
    Int256,
    UInt8,
    UInt16,
    UInt32,
    UInt64,
    UInt128,
    UInt256,
    
    // 浮点类型
    Float32,
    Float64,
    Decimal,
    
    // 布尔类型
    Bool,
    
    // 字符串类型
    String,
    FixedString,
    
    // 日期时间类型
    Date,
    Date32,
    DateTime,
    DateTime64,
    
    // 枚举类型
    Enum,
    Enum8,
    Enum16,
    
    // 数组和嵌套类型
    Array,
    Nested,
    Tuple,
    
    // 特殊类型
    UUID,
    IPv4,
    IPv6,
    
    // 地理类型
    Point,
    Ring,
    Polygon,
    MultiPolygon,
    
    // 聚合函数类型
    AggregateFunction,
    SimpleAggregateFunction,
    
    // 低基数类型
    LowCardinality,
    
    // 可为空类型
    Nullable
}
