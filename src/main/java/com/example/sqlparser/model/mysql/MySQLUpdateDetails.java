package com.example.sqlparser.model.mysql;

import com.example.sqlparser.model.UpdateDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MySQL 特有的 UPDATE 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLUpdateDetails extends UpdateDetails {
    
    // LOW_PRIORITY / HIGH_PRIORITY
    private String priority;
    
    // IGNORE
    private boolean ignore;
    
    // LIMIT (MySQL 特有的简单 LIMIT)
    private Long limitCount;
    
    // ORDER BY (多表 UPDATE 支持)
    private List<String> orderByColumns;
}
