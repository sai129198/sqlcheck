package com.example.sqlparser.model.presto;

import lombok.Data;

/**
 * Presto 列定义
 */
@Data
public class PrestoColumnDef {
    private String name;
    private String dataType;
    private boolean nullable;
    private String comment;
    private String defaultValue;
    
    // 复杂类型的类型参数
    private String typeParameters;
}
