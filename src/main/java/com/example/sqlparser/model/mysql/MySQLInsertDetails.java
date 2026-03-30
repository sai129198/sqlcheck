package com.example.sqlparser.model.mysql;

import com.example.sqlparser.model.InsertDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MySQL 特有的 INSERT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLInsertDetails extends InsertDetails {
    
    // INSERT IGNORE
    private boolean ignore;
    
    // REPLACE (MySQL 特有)
    private boolean replace;
    
    // ON DUPLICATE KEY UPDATE
    private List<DuplicateKeyUpdate> onDuplicateKeyUpdates;
    
    // LOW_PRIORITY / HIGH_PRIORITY / DELAYED
    private String priority;  // LOW_PRIORITY, HIGH_PRIORITY, DELAYED
    
    // SET 语法 (INSERT INTO ... SET col=val, ...)
    private boolean setSyntax;
    private List<SetAssignment> setAssignments;
    
    @Data
    public static class DuplicateKeyUpdate {
        private String column;
        private String expression;
    }
    
    @Data
    public static class SetAssignment {
        private String column;
        private String value;
    }
}
