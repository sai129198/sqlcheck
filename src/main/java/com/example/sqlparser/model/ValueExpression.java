package com.example.sqlparser.model;

import lombok.Data;

import java.util.List;

/**
 * 值表达式（用于 VALUES）
 */
@Data
public class ValueExpression {
    private ValueType type;
    private String value;
    private LineageInfo expression;
    
    // 多行 VALUES
    public static List<ValueExpression> row(List<ValueExpression> values) {
        return values;
    }
}
