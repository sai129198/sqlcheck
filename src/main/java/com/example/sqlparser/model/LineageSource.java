package com.example.sqlparser.model;

import lombok.Data;

/**
 * 血缘来源
 */
@Data
public class LineageSource {
    private SourceType type;
    
    // 根据 type 填充
    private TableColumnRef tableColumn;
    private CteColumnRef cteColumn;
    private SubQueryColumnRef subQueryColumn;
    private ConstantValue constant;
    private LineageInfo nestedExpression;
    
    // 位置信息
    private SourceLocation location;
}
