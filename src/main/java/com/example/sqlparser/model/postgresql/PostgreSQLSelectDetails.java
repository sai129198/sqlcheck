package com.example.sqlparser.model.postgresql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.example.sqlparser.model.SelectDetails;

/**
 * PostgreSQL SELECT 特有详情
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgreSQLSelectDetails extends SelectDetails {

    // DISTINCT ON
    private boolean distinctOn;
    private String distinctOnExpression;

    // LIMIT / OFFSET 选项
    private boolean limitAll;  // LIMIT ALL
    private String offsetExpression;
    private String limitExpression;
    private boolean withTies;  // FETCH FIRST ... WITH TIES

    // 锁定子句
    private boolean forUpdate;
    private boolean forNoKeyUpdate;
    private boolean forShare;
    private boolean forKeyShare;
    private String forUpdateOf;  // OF table_name
    private boolean forUpdateNowait;  // NOWAIT
    private boolean forUpdateSkipLocked;  // SKIP LOCKED

    // TABLESPACE
    private String tablespace;

    // 窗口函数
    private boolean hasWindowFunctions;

    // CTE
    private boolean hasCte;
}
