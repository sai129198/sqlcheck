package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * INSERT 语句详情
 */
@Data
public class InsertDetails {
    private TargetTable targetTable;
    private List<String> targetColumns = new ArrayList<>();
    private InsertMode mode;
    
    // VALUES 模式
    private List<List<ValueExpression>> valueRows = new ArrayList<>();
    
    // SELECT 模式
    private QueryBlock selectQuery;
    
    // 字段映射血缘
    private List<ColumnMapping> columnMappings = new ArrayList<>();
    
    // 冲突处理
    private OnConflictAction onConflict;
}
