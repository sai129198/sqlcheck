package com.example.sqlparser.model.mysql;

import com.example.sqlparser.model.DeleteDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MySQL 特有的 DELETE 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLDeleteDetails extends DeleteDetails {
    
    // LOW_PRIORITY / HIGH_PRIORITY
    private String priority;
    
    // QUICK
    private boolean quick;
    
    // IGNORE
    private boolean ignore;
    
    // LIMIT (MySQL 特有的简单 LIMIT)
    private Long limitCount;
    
    // ORDER BY
    private List<String> orderByColumns;
    
    // 多表 DELETE
    private boolean multiTable;
    private List<String> tablesToDelete;
}
