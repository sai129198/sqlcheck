package com.example.sqlparser.model.impala;

/**
 * Impala 压缩格式枚举
 */
public enum ImpalaCompression {
    NONE,
    SNAPPY,
    GZIP,
    BZIP2,
    DEFLATE,
    LZO,
    ZSTD
}
