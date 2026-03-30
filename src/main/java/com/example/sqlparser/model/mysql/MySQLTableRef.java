package com.example.sqlparser.model.mysql;

import com.example.sqlparser.model.TableRef;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MySQL 表引用
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLTableRef extends TableRef {
    
    // 是否为临时表
    private boolean temporary;
    
    // 存储引擎
    private String engine;
    
    // 字符集
    private String charset;
    
    // 排序规则
    private String collation;
    
    // 自增起始值
    private long autoIncrement;
    
    // 表注释
    private String comment;
    
    // 分区信息
    private MySQLPartitionInfo partitionInfo;
    
    // 索引信息
    private List<MySQLIndex> indexes;
    
    @Data
    public static class MySQLIndex {
        private String name;
        private String type;  // PRIMARY, UNIQUE, INDEX, FULLTEXT, SPATIAL
        private List<String> columns;
    }
}
