package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 血缘信息
 */
@Data
public class LineageInfo {
    private ExpressionType expressionType;
    private List<LineageSource> sources = new ArrayList<>();
    private String originalExpression;
    private List<TransformFunction> transforms = new ArrayList<>();
}
