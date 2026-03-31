package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.postgresql.*;

import java.util.*;
import java.util.regex.*;

/**
 * PostgreSQL SQL 解析器
 * 扩展基础 SQL 解析器，支持 PostgreSQL 特有语法
 */
public class PostgreSQLSqlParser extends SqlParser {
    
    // PostgreSQL 特有的正则模式
    private static final Pattern LIMIT_OFFSET_PATTERN = Pattern.compile(
        "LIMIT\\s+(ALL|\\d+|\\?)\\s*(?:OFFSET\\s+(\\d+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FETCH_OFFSET_PATTERN = Pattern.compile(
        "(?:OFFSET\\s+(\\d+)\\s*(?:ROW|ROWS)?)?\\s*FETCH\\s+(?:FIRST|NEXT)\\s+(\\d+)\\s*(?:ROW|ROWS)\\s*(?:ONLY|WITH\\s+TIES)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DISTINCT_ON_PATTERN = Pattern.compile(
        "SELECT\\s+DISTINCT\\s+ON\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile(
        "FOR\\s+(UPDATE|NO\\s+KEY\\s+UPDATE|SHARE|KEY\\s+SHARE)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern RETURNING_PATTERN = Pattern.compile(
        "RETURNING\\s+(.+?)(?:$|;)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ON_CONFLICT_PATTERN = Pattern.compile(
        "ON\\s+CONFLICT\\s*(?:\\(([^)]+)\\))?\\s*(DO\\s+NOTHING|DO\\s+UPDATE\\s+SET)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CTE_PATTERN = Pattern.compile(
        "WITH\\s+(?:RECURSIVE\\s+)?(.+?)\\s*SELECT",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    /**
     * 解析 PostgreSQL CREATE TABLE
     */
    public PostgreSQLCreateTable parseCreateTable(String sql) {
        PostgreSQLCreateTable createTable = new PostgreSQLCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测 TEMPORARY / TEMP
        createTable.setTemporary(upper.contains("CREATE TEMPORARY") || upper.contains("CREATE TEMP"));
        
        // 检测 UNLOGGED
        createTable.setUnlogged(upper.contains("CREATE UNLOGGED"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析约束
        createTable.setConstraints(parseConstraints(sql));
        
        // 解析索引
        createTable.setIndexes(parseIndexes(sql));
        
        // 解析表选项
        parseTableOptions(sql, createTable);
        
        // 解析分区
        parsePartitionInfo(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        // 解析 LIKE
        parseLike(sql, createTable);
        
        // 解析 INHERITS
        parseInherits(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, PostgreSQLCreateTable createTable) {
        // 尝试匹配 schema.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+|TEMP\\s+|UNLOGGED\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setSchema(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+|TEMP\\s+|UNLOGGED\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<PostgreSQLColumnDef> parseColumns(String sql) {
        List<PostgreSQLColumnDef> columns = new ArrayList<>();

        // 提取括号内的内容
        int start = sql.indexOf('(');
        int end = sql.lastIndexOf(')');
        if (start < 0 || end < 0 || start >= end) {
            return columns;
        }

        String colSection = sql.substring(start + 1, end);

        // 按逗号分割，但要排除复杂表达式内的逗号
        String[] colDefs = smartSplit(colSection);

        for (String colDef : colDefs) {
            colDef = colDef.trim();
            if (colDef.isEmpty()) continue;

            // 跳过约束定义
            if (isConstraintDefinition(colDef)) {
                continue;
            }

            PostgreSQLColumnDef column = parseColumnDef(colDef);
            if (column != null) {
                columns.add(column);
            }
        }

        return columns;
    }
    
    /**
     * 智能分割列定义
     */
    private String[] smartSplit(String colSection) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        
        for (char c : colSection.toCharArray()) {
            if (c == '(' || c == '[') {
                depth++;
                current.append(c);
            } else if (c == ')' || c == ']') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        
        return result.toArray(new String[0]);
    }
    
    /**
     * 判断是否约束定义
     */
    private boolean isConstraintDefinition(String colDef) {
        String upper = colDef.toUpperCase().trim();
        return upper.startsWith("CONSTRAINT") ||
               upper.startsWith("PRIMARY KEY") ||
               upper.startsWith("UNIQUE") ||
               upper.startsWith("CHECK") ||
               upper.startsWith("FOREIGN KEY") ||
               upper.startsWith("EXCLUDE");
    }
    
    /**
     * 解析单个列定义
     */
    private PostgreSQLColumnDef parseColumnDef(String colDef) {
        String trimmed = colDef.trim();
        String upper = trimmed.toUpperCase();

        PostgreSQLColumnDef column = new PostgreSQLColumnDef();

        // 提取列名和数据类型 (第一个单词是列名，第二个是数据类型)
        Pattern nameTypePattern = Pattern.compile("^(\\w+)\\s+([\\w\\[\\]()]+)");
        Matcher nameTypeMatcher = nameTypePattern.matcher(trimmed);
        if (!nameTypeMatcher.find()) {
            return null;
        }

        column.setName(nameTypeMatcher.group(1));

        // 解析数据类型（可能包含数组标记）
        String dataType = nameTypeMatcher.group(2);
        if (dataType.contains("[]")) {
            column.setArray(true);
            dataType = dataType.replace("[]", "");
            column.setArrayDimensions(1);
        }
        column.setDataType(dataType);

        // SERIAL 类型检测
        if (dataType.toUpperCase().startsWith("SERIAL") ||
            dataType.toUpperCase().startsWith("BIGSERIAL") ||
            dataType.toUpperCase().startsWith("SMALLSERIAL")) {
            column.setSerial(true);
        }

        // COLLATE
        Pattern collatePattern = Pattern.compile("COLLATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher collateMatcher = collatePattern.matcher(trimmed);
        if (collateMatcher.find()) {
            column.setCollation(collateMatcher.group(1));
        }

        // NOT NULL / NULL (直接检查字符串，不依赖正则分组)
        if (upper.contains("NOT NULL")) {
            column.setNullable(false);
        } else if (upper.contains(" NULL")) {
            column.setNullable(true);
        } else {
            column.setNullable(true);  // 默认允许 NULL
        }

        // DEFAULT
        Pattern defaultPattern = Pattern.compile("DEFAULT\\s+([^\\s,]+(?:\\s+[^\\s,]+)*)", Pattern.CASE_INSENSITIVE);
        Matcher defaultMatcher = defaultPattern.matcher(trimmed);
        if (defaultMatcher.find()) {
            column.setDefaultExpression(defaultMatcher.group(1).trim());
        }

        // IDENTITY
        if (upper.contains("GENERATED ALWAYS AS IDENTITY") ||
            upper.contains("GENERATED BY DEFAULT AS IDENTITY")) {
            column.setIdentity(true);
        }

        // UNIQUE
        if (upper.contains("UNIQUE")) {
            column.setUnique(true);
        }

        // PRIMARY KEY
        if (upper.contains("PRIMARY KEY")) {
            column.setPrimaryKey(true);
        }

        return column;
    }
    
    /**
     * 解析约束
     */
    private List<PostgreSQLCreateTable.PostgreSQLConstraintDef> parseConstraints(String sql) {
        List<PostgreSQLCreateTable.PostgreSQLConstraintDef> constraints = new ArrayList<>();
        
        // PRIMARY KEY
        Pattern pkPattern = Pattern.compile(
            "CONSTRAINT\\s+(\\w+)\\s+PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher pkMatcher = pkPattern.matcher(sql);
        while (pkMatcher.find()) {
            PostgreSQLCreateTable.PostgreSQLConstraintDef constraint = new PostgreSQLCreateTable.PostgreSQLConstraintDef();
            constraint.setName(pkMatcher.group(1));
            constraint.setType("PRIMARY KEY");
            String[] cols = pkMatcher.group(2).split("\\s*,\\s*");
            constraint.setColumns(Arrays.asList(cols));
            constraints.add(constraint);
        }
        
        // 无名 PRIMARY KEY
        pkPattern = Pattern.compile(
            "PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        pkMatcher = pkPattern.matcher(sql);
        while (pkMatcher.find()) {
            PostgreSQLCreateTable.PostgreSQLConstraintDef constraint = new PostgreSQLCreateTable.PostgreSQLConstraintDef();
            constraint.setType("PRIMARY KEY");
            String[] cols = pkMatcher.group(1).split("\\s*,\\s*");
            constraint.setColumns(Arrays.asList(cols));
            constraints.add(constraint);
        }
        
        // UNIQUE
        Pattern uniquePattern = Pattern.compile(
            "CONSTRAINT\\s+(\\w+)\\s+UNIQUE\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher uniqueMatcher = uniquePattern.matcher(sql);
        while (uniqueMatcher.find()) {
            PostgreSQLCreateTable.PostgreSQLConstraintDef constraint = new PostgreSQLCreateTable.PostgreSQLConstraintDef();
            constraint.setName(uniqueMatcher.group(1));
            constraint.setType("UNIQUE");
            String[] cols = uniqueMatcher.group(2).split("\\s*,\\s*");
            constraint.setColumns(Arrays.asList(cols));
            constraints.add(constraint);
        }
        
        // FOREIGN KEY
        Pattern fkPattern = Pattern.compile(
            "CONSTRAINT\\s+(\\w+)\\s+FOREIGN\\s+KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+(\\w+)\\s*\\(([^)]+)\\)(?:\\s+ON\\s+DELETE\\s+(\\w+))?(?:\\s+ON\\s+UPDATE\\s+(\\w+))?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher fkMatcher = fkPattern.matcher(sql);
        while (fkMatcher.find()) {
            PostgreSQLCreateTable.PostgreSQLConstraintDef constraint = new PostgreSQLCreateTable.PostgreSQLConstraintDef();
            constraint.setName(fkMatcher.group(1));
            constraint.setType("FOREIGN KEY");
            String[] cols = fkMatcher.group(2).split("\\s*,\\s*");
            constraint.setColumns(Arrays.asList(cols));
            constraint.setRefTable(fkMatcher.group(3));
            String[] refCols = fkMatcher.group(4).split("\\s*,\\s*");
            constraint.setRefColumns(Arrays.asList(refCols));
            if (fkMatcher.group(5) != null) {
                constraint.setOnDelete(fkMatcher.group(5));
            }
            if (fkMatcher.group(6) != null) {
                constraint.setOnUpdate(fkMatcher.group(6));
            }
            constraints.add(constraint);
        }
        
        // CHECK
        Pattern checkPattern = Pattern.compile(
            "CONSTRAINT\\s+(\\w+)\\s+CHECK\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher checkMatcher = checkPattern.matcher(sql);
        while (checkMatcher.find()) {
            PostgreSQLCreateTable.PostgreSQLConstraintDef constraint = new PostgreSQLCreateTable.PostgreSQLConstraintDef();
            constraint.setName(checkMatcher.group(1));
            constraint.setType("CHECK");
            constraint.setExpression(checkMatcher.group(2));
            constraints.add(constraint);
        }
        
        return constraints;
    }
    
    /**
     * 解析索引
     */
    private List<PostgreSQLCreateTable.PostgreSQLIndexDef> parseIndexes(String sql) {
        List<PostgreSQLCreateTable.PostgreSQLIndexDef> indexes = new ArrayList<>();
        
        // CREATE INDEX 语句中的索引（简化处理）
        // 实际应该在单独的 CREATE INDEX 语句中解析
        
        return indexes;
    }
    
    /**
     * 解析表选项
     */
    private void parseTableOptions(String sql, PostgreSQLCreateTable createTable) {
        // TABLESPACE
        Matcher m = Pattern.compile("TABLESPACE\\s+(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setTablespace(m.group(1));
        }
    }
    
    /**
     * 解析分区信息
     */
    private void parsePartitionInfo(String sql, PostgreSQLCreateTable createTable) {
        // 分区表
        Pattern partitionPattern = Pattern.compile(
            "PARTITION\\s+BY\\s+(RANGE|LIST|HASH)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = partitionPattern.matcher(sql);
        if (m.find()) {
            PostgreSQLPartitionInfo partitionInfo = new PostgreSQLPartitionInfo();
            partitionInfo.setPartitioned(true);
            partitionInfo.setPartitionType(m.group(1).toUpperCase());
            partitionInfo.setPartitionKey(m.group(2));
            createTable.setPartitionInfo(partitionInfo);
        }
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, PostgreSQLCreateTable createTable) {
        int asIndex = sql.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            String asPart = sql.substring(asIndex + 4).trim();
            if (asPart.toUpperCase().startsWith("SELECT")) {
                createTable.setAsSelect(asPart);
                
                // 检测 WITH DATA / WITH NO DATA
                if (asPart.toUpperCase().contains("WITH NO DATA")) {
                    createTable.setWithData(false);
                }
            }
        }
    }
    
    /**
     * 解析 LIKE
     */
    private void parseLike(String sql, PostgreSQLCreateTable createTable) {
        Matcher m = Pattern.compile(
            "LIKE\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            if (m.group(1) != null) {
                createTable.setLikeTable(m.group(1) + "." + m.group(2));
            } else {
                createTable.setLikeTable(m.group(2));
            }
        }
    }
    
    /**
     * 解析 INHERITS
     */
    private void parseInherits(String sql, PostgreSQLCreateTable createTable) {
        Matcher m = Pattern.compile(
            "INHERITS\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] tables = m.group(1).split("\\s*,\\s*");
            createTable.setInherits(Arrays.asList(tables));
        }
    }
    
    /**
     * 重写 parse 方法，支持 PostgreSQL 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 PostgreSQL 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        } else if (stmt.isUpdate()) {
            extendUpdateDetails(stmt, sql);
        } else if (stmt.isDelete()) {
            extendDeleteDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 PostgreSQL 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PostgreSQLSelectDetails pgDetails = new PostgreSQLSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            pgDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            pgDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            pgDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 DISTINCT ON
        Matcher distinctOnMatcher = DISTINCT_ON_PATTERN.matcher(sql);
        if (distinctOnMatcher.find()) {
            pgDetails.setDistinctOn(true);
            pgDetails.setDistinctOnExpression(distinctOnMatcher.group(1));
        }
        
        // 检测 LIMIT / OFFSET
        Matcher limitMatcher = LIMIT_OFFSET_PATTERN.matcher(sql);
        if (limitMatcher.find()) {
            String limitValue = limitMatcher.group(1);
            if ("ALL".equalsIgnoreCase(limitValue)) {
                pgDetails.setLimitAll(true);
            } else if (limitValue != null) {
                pgDetails.setLimitExpression(limitValue);
            }
            if (limitMatcher.group(2) != null) {
                pgDetails.setOffsetExpression(limitMatcher.group(2));
            }
        }
        
        // 检测 FETCH / OFFSET (SQL 标准语法)
        Matcher fetchMatcher = FETCH_OFFSET_PATTERN.matcher(sql);
        if (fetchMatcher.find()) {
            if (fetchMatcher.group(1) != null) {
                pgDetails.setOffsetExpression(fetchMatcher.group(1));
            }
            pgDetails.setLimitExpression(fetchMatcher.group(2));
            if (upper.contains("WITH TIES")) {
                pgDetails.setWithTies(true);
            }
        }
        
        // 检测锁定子句
        Matcher forUpdateMatcher = FOR_UPDATE_PATTERN.matcher(sql);
        if (forUpdateMatcher.find()) {
            String lockType = forUpdateMatcher.group(1).toUpperCase().replaceAll("\\s+", " ");
            switch (lockType) {
                case "UPDATE":
                    pgDetails.setForUpdate(true);
                    break;
                case "NO KEY UPDATE":
                    pgDetails.setForNoKeyUpdate(true);
                    break;
                case "SHARE":
                    pgDetails.setForShare(true);
                    break;
                case "KEY SHARE":
                    pgDetails.setForKeyShare(true);
                    break;
            }
        }
        
        // 检测 NOWAIT / SKIP LOCKED
        if (upper.contains("NOWAIT")) {
            pgDetails.setForUpdateNowait(true);
        }
        if (upper.contains("SKIP LOCKED")) {
            pgDetails.setForUpdateSkipLocked(true);
        }
        
        // 检测 OF table_name
        Matcher ofMatcher = Pattern.compile("FOR\\s+UPDATE\\s+OF\\s+(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (ofMatcher.find()) {
            pgDetails.setForUpdateOf(ofMatcher.group(1));
        }
        
        // 检测 CTE
        Matcher cteMatcher = CTE_PATTERN.matcher(sql);
        if (cteMatcher.find()) {
            pgDetails.setHasCte(true);
        }
        
        stmt.setSelectDetails(pgDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 PostgreSQL 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PostgreSQLInsertDetails pgDetails = new PostgreSQLInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), pgDetails);
        }
        
        // 检测 ON CONFLICT
        Matcher conflictMatcher = ON_CONFLICT_PATTERN.matcher(sql);
        if (conflictMatcher.find()) {
            pgDetails.setOnConflict(true);
            if (conflictMatcher.group(1) != null) {
                pgDetails.setOnConflictTarget(conflictMatcher.group(1));
            }
            String action = conflictMatcher.group(2).toUpperCase();
            pgDetails.setOnConflictAction(action);
            
            // 解析 DO UPDATE SET
            if (action.contains("UPDATE")) {
                parseOnConflictUpdates(sql, pgDetails);
            }
        }
        
        // 检测 RETURNING
        Matcher returningMatcher = RETURNING_PATTERN.matcher(sql);
        if (returningMatcher.find()) {
            pgDetails.setReturning(true);
            String returningCols = returningMatcher.group(1).trim();
            if ("*".equals(returningCols)) {
                pgDetails.setReturningColumns(Arrays.asList("*"));
            } else {
                String[] cols = returningCols.split("\\s*,\\s*");
                pgDetails.setReturningColumns(Arrays.asList(cols));
            }
        }
        
        // 检测 OVERRIDING
        Matcher overridingMatcher = Pattern.compile(
            "OVERRIDING\\s+(SYSTEM|USER)\\s+VALUE",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        if (overridingMatcher.find()) {
            pgDetails.setOverriding(overridingMatcher.group(1).toUpperCase() + " VALUE");
        }
        
        stmt.setInsertDetails(pgDetails);
    }
    
    /**
     * 解析 ON CONFLICT DO UPDATE SET
     */
    private void parseOnConflictUpdates(String sql, PostgreSQLInsertDetails pgDetails) {
        List<PostgreSQLInsertDetails.OnConflictUpdate> updates = new ArrayList<>();
        
        // 提取 SET 子句
        Pattern setPattern = Pattern.compile(
            "DO\\s+UPDATE\\s+SET\\s+(.+?)(?:\\s+WHERE\\s+|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher m = setPattern.matcher(sql);
        if (m.find()) {
            String setClause = m.group(1).trim();
            String[] assignments = smartSplit(setClause);
            
            for (String assignment : assignments) {
                String[] parts = assignment.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    PostgreSQLInsertDetails.OnConflictUpdate update = new PostgreSQLInsertDetails.OnConflictUpdate();
                    update.setColumn(parts[0].trim());
                    update.setExpression(parts[1].trim());
                    updates.add(update);
                }
            }
        }
        
        pgDetails.setOnConflictUpdates(updates);
    }
    
    /**
     * 扩展 UPDATE 详情为 PostgreSQL 特有
     */
    private void extendUpdateDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PostgreSQLUpdateDetails pgDetails = new PostgreSQLUpdateDetails();
        
        // 复制基础信息
        if (stmt.getUpdateDetails() != null) {
            copyUpdateDetails(stmt.getUpdateDetails(), pgDetails);
        }
        
        // 检测 FROM 子句
        Pattern fromPattern = Pattern.compile(
            "FROM\\s+(.+?)(?:\\s+WHERE\\s+|\\s+RETURNING\\s+|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher fromMatcher = fromPattern.matcher(sql);
        if (fromMatcher.find()) {
            pgDetails.setHasFrom(true);
            pgDetails.setFromClauseString(fromMatcher.group(1).trim());
        }
        
        // 检测 RETURNING
        Matcher returningMatcher = RETURNING_PATTERN.matcher(sql);
        if (returningMatcher.find()) {
            pgDetails.setReturning(true);
            pgDetails.setReturningColumns(returningMatcher.group(1).trim());
        }
        
        // 检测 CTE
        Matcher cteMatcher = CTE_PATTERN.matcher(sql);
        if (cteMatcher.find()) {
            pgDetails.setWithCte(true);
            pgDetails.setCteExpression(cteMatcher.group(1).trim());
        }
        
        stmt.setUpdateDetails(pgDetails);
    }
    
    /**
     * 扩展 DELETE 详情为 PostgreSQL 特有
     */
    private void extendDeleteDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PostgreSQLDeleteDetails pgDetails = new PostgreSQLDeleteDetails();
        
        // 复制基础信息
        if (stmt.getDeleteDetails() != null) {
            copyDeleteDetails(stmt.getDeleteDetails(), pgDetails);
        }
        
        // 检测 USING 子句
        Pattern usingPattern = Pattern.compile(
            "USING\\s+(.+?)(?:\\s+WHERE\\s+|\\s+RETURNING\\s+|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        Matcher usingMatcher = usingPattern.matcher(sql);
        if (usingMatcher.find()) {
            pgDetails.setHasUsing(true);
            pgDetails.setUsingClauseString(usingMatcher.group(1).trim());
        }
        
        // 检测 RETURNING
        Matcher returningMatcher = RETURNING_PATTERN.matcher(sql);
        if (returningMatcher.find()) {
            pgDetails.setReturning(true);
            pgDetails.setReturningColumns(returningMatcher.group(1).trim());
        }
        
        // 检测 CTE
        Matcher cteMatcher = CTE_PATTERN.matcher(sql);
        if (cteMatcher.find()) {
            pgDetails.setWithCte(true);
            pgDetails.setCteExpression(cteMatcher.group(1).trim());
        }
        
        stmt.setDeleteDetails(pgDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, PostgreSQLInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
    
    /**
     * 复制 UpdateDetails 属性
     */
    private void copyUpdateDetails(UpdateDetails source, PostgreSQLUpdateDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setAssignments(source.getAssignments());
        target.setFromClause(source.getFromClause());
        target.setJoins(source.getJoins());
        target.setWhere(source.getWhere());
    }
    
    /**
     * 复制 DeleteDetails 属性
     */
    private void copyDeleteDetails(DeleteDetails source, PostgreSQLDeleteDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setUsingClause(source.getUsingClause());
        target.setJoins(source.getJoins());
        target.setWhere(source.getWhere());
    }
}
