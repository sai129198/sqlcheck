package com.example.sqlparser.model.postgresql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.sqlparser.model.InsertDetails;
import java.util.List;

/**
 * PostgreSQL INSERT 特有详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgreSQLInsertDetails extends InsertDetails {
    
    // ON CONFLICT (UPSERT)
    private boolean onConflict;
    private String onConflictTarget;  // ON CONFLICT (columns)
    private String onConflictAction;  // DO NOTHING / DO UPDATE
    private List<OnConflictUpdate> onConflictUpdates;  // DO UPDATE SET ...
    private String onConflictWhere;  // WHERE condition for DO UPDATE
    
    // RETURNING
    private boolean returning;
    private List<String> returningColumns;
    
    // OVERRIDING
    private String overriding;  // OVERRIDING SYSTEM VALUE / OVERRIDING USER VALUE
    
    @Data
    public static class OnConflictUpdate {
        private String column;
        private String expression;
    }
}
