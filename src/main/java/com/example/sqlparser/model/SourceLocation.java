package com.example.sqlparser.model;

import lombok.Data;

/**
 * 源代码位置
 */
@Data
public class SourceLocation {
    private int line;
    private int column;
    private int startIndex;
    private int stopIndex;
}
