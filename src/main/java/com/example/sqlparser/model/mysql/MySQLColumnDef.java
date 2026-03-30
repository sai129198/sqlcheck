package com.example.sqlparser.model.mysql;

import lombok.Data;

/**
 * MySQL 列定义
 */
@Data
public class MySQLColumnDef {
    private String name;
    private String dataType;
    private boolean nullable;
    private String defaultValue;
    private String comment;
    
    // 自增
    private boolean autoIncrement;
    
    // 字符集
    private String charset;
    private String collation;
    
    // 显示宽度 (如 INT(11))
    private int displayWidth;
    
    // 无符号
    private boolean unsigned;
    
    // 零填充
    private boolean zerofill;
    
    // 生成列
    private boolean generated;
    private String generationExpression;
    
    // 虚拟列/存储列
    private String generatedType;  // VIRTUAL or STORED
    
    // ON UPDATE
    private String onUpdate;
}
