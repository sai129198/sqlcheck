package com.example.sqlparser.model.hive;

import lombok.Data;

/**
 * Hive 列定义
 */
@Data
public class HiveColumnDef {
    private String name;
    private String dataType;
    private String comment;
    
    // 复杂类型的类型参数
    private String typeParameters;
}
