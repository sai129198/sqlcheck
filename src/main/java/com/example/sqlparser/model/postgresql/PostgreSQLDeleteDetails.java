package com.example.sqlparser.model.postgresql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.sqlparser.model.DeleteDetails;

/**
 * PostgreSQL DELETE 特有详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgreSQLDeleteDetails extends DeleteDetails {
    
    // USING 子句字符串表示 (代替 MySQL 的多表 DELETE)
    private boolean hasUsing;
    private String usingClauseString;
    
    // RETURNING
    private boolean returning;
    private String returningColumns;
    
    // WITH 子句 (CTE)
    private boolean withCte;
    private String cteExpression;
}
