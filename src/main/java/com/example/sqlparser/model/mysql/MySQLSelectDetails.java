package com.example.sqlparser.model.mysql;

import com.example.sqlparser.model.SelectDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * MySQL 特有的 SELECT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MySQLSelectDetails extends SelectDetails {
    
    // SQL_CALC_FOUND_ROWS
    private boolean calcFoundRows;
    
    // SQL_NO_CACHE / SQL_CACHE
    private boolean noCache;
    private boolean cache;
    
    // STRAIGHT_JOIN
    private boolean straightJoin;
    
    // HIGH_PRIORITY
    private boolean highPriority;
    
    // LOCK IN SHARE MODE / FOR UPDATE
    private boolean lockInShareMode;
    private boolean forUpdate;
    
    // LIMIT offset, count 语法
    private Long limitOffset;
    private Long limitCount;
    
    // PROCEDURE (存储过程调用)
    private String procedure;
    
    // INTO OUTFILE / INTO DUMPFILE / INTO var_name
    private IntoClause intoClause;
    
    @Data
    public static class IntoClause {
        private String type;  // OUTFILE, DUMPFILE, VARIABLE
        private String target;
        private String characterSet;
        private String fieldsTerminatedBy;
        private String linesTerminatedBy;
    }
}
