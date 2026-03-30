package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.mysql.*;

import java.util.*;
import java.util.regex.*;

/**
 * MySQL SQL 解析器
 * 扩展基础 SQL 解析器，支持 MySQL 特有语法
 */
public class MySQLSqlParser extends SqlParser {
    
    // MySQL 特有的正则模式
    private static final Pattern LIMIT_OFFSET_PATTERN = Pattern.compile(
        "LIMIT\\s+(?:(\\d+)\\s*,\\s*)?(\\d+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSERT_IGNORE_PATTERN = Pattern.compile(
        "INSERT\\s+(IGNORE|LOW_PRIORITY|HIGH_PRIORITY|DELAYED)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ON_DUPLICATE_KEY_PATTERN = Pattern.compile(
        "ON\\s+DUPLICATE\\s+KEY\\s+UPDATE\\s+(.+?)(?:$|;)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SELECT_OPTION_PATTERN = Pattern.compile(
        "SELECT\\s+(ALL|DISTINCT|DISTINCTROW)?\\s*(SQL_CALC_FOUND_ROWS|SQL_NO_CACHE|SQL_CACHE|STRAIGHT_JOIN|HIGH_PRIORITY)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile(
        "FOR\\s+UPDATE",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LOCK_IN_SHARE_MODE_PATTERN = Pattern.compile(
        "LOCK\\s+IN\\s+SHARE\\s+MODE",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UPDATE_PRIORITY_PATTERN = Pattern.compile(
        "UPDATE\\s+(LOW_PRIORITY|HIGH_PRIORITY|IGNORE)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DELETE_PRIORITY_PATTERN = Pattern.compile(
        "DELETE\\s+(LOW_PRIORITY|QUICK|IGNORE)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 MySQL CREATE TABLE
     */
    public MySQLCreateTable parseCreateTable(String sql) {
        MySQLCreateTable createTable = new MySQLCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测 TEMPORARY
        createTable.setTemporary(upper.contains("CREATE TEMPORARY"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
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
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, MySQLCreateTable createTable) {
        // 尝试匹配 database.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setDatabase(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<MySQLColumnDef> parseColumns(String sql) {
        List<MySQLColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分（在第一个括号内，排除索引定义）
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+[\\w()\\s]+(?:\\s+NOT\\s+NULL|\\s+NULL)?(?:\\s+AUTO_INCREMENT)?(?:\\s+DEFAULT\\s+[^,]+)?(?:\\s+ON\\s+UPDATE\\s+[^,]+)?(?:\\s+COMMENT\\s+'[^']*')?,?\\s*)+)\\s*[,)]",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            // 按逗号分割，但要排除函数内的逗号
            String[] colDefs = colSection.split("\\s*,\\s*(?=\\w+\\s+\\w)");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                
                // 跳过索引定义
                if (colDef.toUpperCase().startsWith("PRIMARY KEY") ||
                    colDef.toUpperCase().startsWith("UNIQUE") ||
                    colDef.toUpperCase().startsWith("KEY") ||
                    colDef.toUpperCase().startsWith("INDEX") ||
                    colDef.toUpperCase().startsWith("FULLTEXT") ||
                    colDef.toUpperCase().startsWith("SPATIAL") ||
                    colDef.toUpperCase().startsWith("CONSTRAINT")) {
                    continue;
                }
                
                MySQLColumnDef column = parseColumnDef(colDef);
                if (column != null) {
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }
    
    /**
     * 解析单个列定义
     */
    private MySQLColumnDef parseColumnDef(String colDef) {
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w()]+)(?:\\s+(UNSIGNED))?(?:\\s+(ZEROFILL))?(?:\\s+(NOT\\s+NULL|NULL))?(?:\\s+DEFAULT\\s+([^\\s,]+))?(?:\\s+ON\\s+UPDATE\\s+([^\\s,]+))?(?:\\s+(AUTO_INCREMENT))?(?:\\s+COMMENT\\s+'([^']*)')?",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            MySQLColumnDef column = new MySQLColumnDef();
            column.setName(m.group(1));
            column.setDataType(m.group(2));
            
            if (m.group(3) != null) {
                column.setUnsigned(true);
            }
            
            if (m.group(4) != null) {
                column.setZerofill(true);
            }
            
            if (m.group(5) != null) {
                column.setNullable(!m.group(5).toUpperCase().contains("NOT"));
            } else {
                column.setNullable(true);
            }
            
            if (m.group(6) != null) {
                column.setDefaultValue(m.group(6));
            }
            
            if (m.group(7) != null) {
                column.setOnUpdate(m.group(7));
            }
            
            if (m.group(8) != null) {
                column.setAutoIncrement(true);
            }
            
            if (m.group(9) != null) {
                column.setComment(m.group(9));
            }
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析索引
     */
    private List<MySQLCreateTable.MySQLIndexDef> parseIndexes(String sql) {
        List<MySQLCreateTable.MySQLIndexDef> indexes = new ArrayList<>();
        
        // PRIMARY KEY
        Pattern pkPattern = Pattern.compile(
            "PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher pkMatcher = pkPattern.matcher(sql);
        if (pkMatcher.find()) {
            MySQLCreateTable.MySQLIndexDef index = new MySQLCreateTable.MySQLIndexDef();
            index.setType("PRIMARY KEY");
            index.setName("PRIMARY");
            String[] cols = pkMatcher.group(1).split("\\s*,\\s*");
            index.setColumns(Arrays.asList(cols));
            indexes.add(index);
        }
        
        // UNIQUE KEY
        Pattern uniquePattern = Pattern.compile(
            "UNIQUE(?:\\s+(?:KEY|INDEX))?\\s+(?:`(\\w+)`|(\\w+))?\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher uniqueMatcher = uniquePattern.matcher(sql);
        while (uniqueMatcher.find()) {
            MySQLCreateTable.MySQLIndexDef index = new MySQLCreateTable.MySQLIndexDef();
            index.setType("UNIQUE");
            index.setName(uniqueMatcher.group(1) != null ? uniqueMatcher.group(1) : uniqueMatcher.group(2));
            String[] cols = uniqueMatcher.group(3).split("\\s*,\\s*");
            index.setColumns(Arrays.asList(cols));
            indexes.add(index);
        }
        
        // INDEX / KEY
        Pattern indexPattern = Pattern.compile(
            "(?:INDEX|KEY)\\s+(?:`(\\w+)`|(\\w+))?\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher indexMatcher = indexPattern.matcher(sql);
        while (indexMatcher.find()) {
            MySQLCreateTable.MySQLIndexDef index = new MySQLCreateTable.MySQLIndexDef();
            index.setType("INDEX");
            index.setName(indexMatcher.group(1) != null ? indexMatcher.group(1) : indexMatcher.group(2));
            String[] cols = indexMatcher.group(3).split("\\s*,\\s*");
            index.setColumns(Arrays.asList(cols));
            indexes.add(index);
        }
        
        return indexes;
    }
    
    /**
     * 解析表选项
     */
    private void parseTableOptions(String sql, MySQLCreateTable createTable) {
        // ENGINE
        Matcher m = Pattern.compile("ENGINE\\s*=\\s*(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setEngine(m.group(1));
        }
        
        // CHARSET / CHARACTER SET
        m = Pattern.compile("(?:DEFAULT\\s+)?(?:CHARSET|CHARACTER\\s+SET)\\s*=\\s*(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setCharset(m.group(1));
        }
        
        // COLLATE
        m = Pattern.compile("(?:DEFAULT\\s+)?COLLATE\\s*=\\s*(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setCollation(m.group(1));
        }
        
        // AUTO_INCREMENT
        m = Pattern.compile("AUTO_INCREMENT\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setAutoIncrement(Long.parseLong(m.group(1)));
        }
        
        // COMMENT
        m = Pattern.compile("COMMENT\\s*=\\s*'([^']*)'", Pattern.CASE_INSENSITIVE).matcher(sql);
        if (m.find()) {
            createTable.setComment(m.group(1));
        }
    }
    
    /**
     * 解析分区信息
     */
    private void parsePartitionInfo(String sql, MySQLCreateTable createTable) {
        // 简化实现，实际分区解析较复杂
        if (sql.toUpperCase().contains("PARTITION BY")) {
            MySQLPartitionInfo partitionInfo = new MySQLPartitionInfo();
            // 这里可以扩展详细的分区解析
            createTable.setPartitionInfo(partitionInfo);
        }
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, MySQLCreateTable createTable) {
        int asIndex = sql.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            String asPart = sql.substring(asIndex + 4).trim();
            if (asPart.toUpperCase().startsWith("SELECT")) {
                createTable.setAsSelect(asPart);
            }
        }
    }
    
    /**
     * 解析 LIKE
     */
    private void parseLike(String sql, MySQLCreateTable createTable) {
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
     * 重写 parse 方法，支持 MySQL 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 检测 REPLACE
        if (upper.startsWith("REPLACE")) {
            return parseReplace(sql);
        }
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 MySQL 特有详情
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
     * 解析 REPLACE 语句（MySQL 特有）
     */
    private SqlStatement parseReplace(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.REPLACE);
        
        MySQLInsertDetails details = new MySQLInsertDetails();
        details.setReplace(true);
        
        // 解析表名和值（复用 INSERT 逻辑）
        // 简化实现
        
        stmt.setInsertDetails(details);
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 MySQL 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        MySQLSelectDetails mysqlDetails = new MySQLSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            mysqlDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            mysqlDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            mysqlDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 SELECT 选项
        Matcher optionMatcher = SELECT_OPTION_PATTERN.matcher(sql);
        if (optionMatcher.find()) {
            String option = optionMatcher.group(2);
            if (option != null) {
                switch (option.toUpperCase()) {
                    case "SQL_CALC_FOUND_ROWS":
                        mysqlDetails.setCalcFoundRows(true);
                        break;
                    case "SQL_NO_CACHE":
                        mysqlDetails.setNoCache(true);
                        break;
                    case "SQL_CACHE":
                        mysqlDetails.setCache(true);
                        break;
                    case "STRAIGHT_JOIN":
                        mysqlDetails.setStraightJoin(true);
                        break;
                    case "HIGH_PRIORITY":
                        mysqlDetails.setHighPriority(true);
                        break;
                }
            }
        }
        
        // 检测 LIMIT offset,count 语法
        Matcher limitMatcher = LIMIT_OFFSET_PATTERN.matcher(sql);
        if (limitMatcher.find()) {
            if (limitMatcher.group(1) != null) {
                mysqlDetails.setLimitOffset(Long.parseLong(limitMatcher.group(1)));
            }
            mysqlDetails.setLimitCount(Long.parseLong(limitMatcher.group(2)));
        }
        
        // 检测 FOR UPDATE
        if (FOR_UPDATE_PATTERN.matcher(sql).find()) {
            mysqlDetails.setForUpdate(true);
        }
        
        // 检测 LOCK IN SHARE MODE
        if (LOCK_IN_SHARE_MODE_PATTERN.matcher(sql).find()) {
            mysqlDetails.setLockInShareMode(true);
        }
        
        stmt.setSelectDetails(mysqlDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 MySQL 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        MySQLInsertDetails mysqlDetails = new MySQLInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), mysqlDetails);
        }
        
        // 检测 IGNORE / LOW_PRIORITY / HIGH_PRIORITY / DELAYED
        Matcher priorityMatcher = INSERT_IGNORE_PATTERN.matcher(sql);
        if (priorityMatcher.find()) {
            String option = priorityMatcher.group(1).toUpperCase();
            switch (option) {
                case "IGNORE":
                    mysqlDetails.setIgnore(true);
                    break;
                case "LOW_PRIORITY":
                case "HIGH_PRIORITY":
                case "DELAYED":
                    mysqlDetails.setPriority(option);
                    break;
            }
        }
        
        // 检测 ON DUPLICATE KEY UPDATE
        Matcher duplicateMatcher = ON_DUPLICATE_KEY_PATTERN.matcher(sql);
        if (duplicateMatcher.find()) {
            List<MySQLInsertDetails.DuplicateKeyUpdate> updates = new ArrayList<>();
            String updateClause = duplicateMatcher.group(1);
            String[] assignments = updateClause.split("\\s*,\\s*");
            
            for (String assignment : assignments) {
                String[] parts = assignment.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    MySQLInsertDetails.DuplicateKeyUpdate update = new MySQLInsertDetails.DuplicateKeyUpdate();
                    update.setColumn(parts[0].trim());
                    update.setExpression(parts[1].trim());
                    updates.add(update);
                }
            }
            mysqlDetails.setOnDuplicateKeyUpdates(updates);
        }
        
        stmt.setInsertDetails(mysqlDetails);
    }
    
    /**
     * 扩展 UPDATE 详情为 MySQL 特有
     */
    private void extendUpdateDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        MySQLUpdateDetails mysqlDetails = new MySQLUpdateDetails();
        
        // 复制基础信息
        if (stmt.getUpdateDetails() != null) {
            copyUpdateDetails(stmt.getUpdateDetails(), mysqlDetails);
        }
        
        // 检测优先级和 IGNORE
        Matcher priorityMatcher = UPDATE_PRIORITY_PATTERN.matcher(sql);
        if (priorityMatcher.find()) {
            String option = priorityMatcher.group(1).toUpperCase();
            if ("IGNORE".equals(option)) {
                mysqlDetails.setIgnore(true);
            } else {
                mysqlDetails.setPriority(option);
            }
        }
        
        // 检测 LIMIT
        Matcher limitMatcher = LIMIT_OFFSET_PATTERN.matcher(sql);
        if (limitMatcher.find()) {
            mysqlDetails.setLimitCount(Long.parseLong(limitMatcher.group(2)));
        }
        
        stmt.setUpdateDetails(mysqlDetails);
    }
    
    /**
     * 扩展 DELETE 详情为 MySQL 特有
     */
    private void extendDeleteDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        MySQLDeleteDetails mysqlDetails = new MySQLDeleteDetails();
        
        // 复制基础信息
        if (stmt.getDeleteDetails() != null) {
            copyDeleteDetails(stmt.getDeleteDetails(), mysqlDetails);
        }
        
        // 检测优先级和选项
        Matcher optionMatcher = DELETE_PRIORITY_PATTERN.matcher(sql);
        if (optionMatcher.find()) {
            String option = optionMatcher.group(1).toUpperCase();
            switch (option) {
                case "LOW_PRIORITY":
                    mysqlDetails.setPriority(option);
                    break;
                case "QUICK":
                    mysqlDetails.setQuick(true);
                    break;
                case "IGNORE":
                    mysqlDetails.setIgnore(true);
                    break;
            }
        }
        
        // 检测 LIMIT
        Matcher limitMatcher = LIMIT_OFFSET_PATTERN.matcher(sql);
        if (limitMatcher.find()) {
            mysqlDetails.setLimitCount(Long.parseLong(limitMatcher.group(2)));
        }
        
        stmt.setDeleteDetails(mysqlDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, MySQLInsertDetails target) {
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
    private void copyUpdateDetails(UpdateDetails source, MySQLUpdateDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setAssignments(source.getAssignments());
        target.setFromClause(source.getFromClause());
        target.setJoins(source.getJoins());
        target.setWhere(source.getWhere());
    }
    
    /**
     * 复制 DeleteDetails 属性
     */
    private void copyDeleteDetails(DeleteDetails source, MySQLDeleteDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setUsingClause(source.getUsingClause());
        target.setJoins(source.getJoins());
        target.setWhere(source.getWhere());
    }
}
