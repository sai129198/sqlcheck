package com.example.sqlparser.model.starrocks;

/**
 * StarRocks 特有的表类型
 */
public enum StarRocksTableType {
    DUPLICATE_KEY,
    AGGREGATE_KEY,
    UNIQUE_KEY,
    PRIMARY_KEY
}
