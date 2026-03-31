package com.example.sqlparser.model.postgresql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.sqlparser.model.UpdateDetails;

/**
 * PostgreSQL UPDATE 特有详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgreSQLUpdateDetails extends UpdateDetails {
    
    // FROM 子句字符串表示 (PostgreSQL 支持多表 UPDATE)
    private boolean hasFrom;
    private String fromClauseString;
    
    // RETURNING
    private boolean returning;
    private String returningColumns;
    
    // WITH 子句 (CTE)
    private boolean withCte;
    private String cteExpression;
}
