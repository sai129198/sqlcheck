package com.example.sqlparser.model;

import lombok.Data;

/**
 * 转换函数
 */
@Data
public class TransformFunction {
    private String name;
    private java.util.List<LineageSource> args;
}
