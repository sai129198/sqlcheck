package com.example.sqlparser.model.impala;

/**
 * Impala 文件格式枚举
 */
public enum ImpalaFileFormat {
    PARQUET,
    TEXTFILE,
    RCFILE,
    SEQUENCEFILE,
    AVRO,
    ORC,
    KUDU,
    HBASE,
    JSON
}
