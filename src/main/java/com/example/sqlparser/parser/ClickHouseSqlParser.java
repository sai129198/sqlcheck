package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.clickhouse.*;

import java.util.*;
import java.util.regex.*;

/**
 * ClickHouse SQL 解析器
 * 扩展基础 SQL 解析器，支持 ClickHouse 特有语法
 */
public class ClickHouseSqlParser extends SqlParser {
    
    // ClickHouse 特有的正则模式
    private static final Pattern ARRAY_JOIN_PATTERN = Pattern.compile(
        "(LEFT\\s+)?ARRAY\\s+JOIN\\s+([^\\s]+)(?:\\s+AS\\s+([^\\s]+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SAMPLE_PATTERN = Pattern.compile(
        "SAMPLE\\s+(?:(\\d+(?:\\.\\d+)?)(?:\\s+OFFSET\\s+(\\d+(?:\\.\\d+)?))?|(\\d+)(?:\\s+OFFSET\\s+(\\d+))?)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LIMIT_N_M_PATTERN = Pattern.compile(
        "LIMIT\\s+(?:(\\d+)\\s*,\\s*)?(\\d+)(?:\\s+WITH\\s+TIES)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FORMAT_PATTERN = Pattern.compile(
        "FORMAT\\s+(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SETTINGS_PATTERN = Pattern.compile(
        "SETTINGS\\s+((?:\\w+\\s*=\\s*[^,]+,?\\s*)+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PREWHERE_PATTERN = Pattern.compile(
        "PREWHERE\\s+(.+?)(?:\\s+(?:GROUP|HAVING|ORDER|LIMIT|FORMAT|SETTINGS|$))",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 ClickHouse CREATE TABLE
     */
    public ClickHouseCreateTable parseCreateTable(String sql) {
        ClickHouseCreateTable createTable = new ClickHouseCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测 TEMPORARY
        createTable.setTemporary(upper.contains("CREATE TEMPORARY"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 检测 OR REPLACE
        createTable.setOrReplace(upper.contains("OR REPLACE"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析引擎
        parseEngine(sql, createTable);
        
        // 解析 PARTITION BY
        parsePartitionBy(sql, createTable);
        
        // 解析 ORDER BY
        parseOrderBy(sql, createTable);
        
        // 解析 PRIMARY KEY
        parsePrimaryKey(sql, createTable);
        
        // 解析 SAMPLE BY
        parseSampleBy(sql, createTable);
        
        // 解析 TTL
        parseTTL(sql, createTable);
        
        // 解析 SETTINGS
        parseTableSettings(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, ClickHouseCreateTable createTable) {
        // 尝试匹配 database.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+|OR\\s+REPLACE\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setDatabase(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+|OR\\s+REPLACE\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<ClickHouseColumnDef> parseColumns(String sql) {
        List<ClickHouseColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分（在第一个括号内）
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+[\\w(),\\s]+(?:\\s+DEFAULT\\s+[^,]+|\\s+MATERIALIZED\\s+[^,]+|\\s+ALIAS\\s+[^,]+)?(?:\\s+COMMENT\\s+'[^']*')?(?:\\s+CODEC\\s*\\([^)]+\\))?,?\\s*)+)\\s*\\)(?:\\s*ENGINE|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            String[] colDefs = colSection.split("\\s*,\\s*(?=\\w+\\s+\\w)");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                
                ClickHouseColumnDef column = parseColumnDef(colDef);
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
    private ClickHouseColumnDef parseColumnDef(String colDef) {
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w(),\\s]+?)(?:\\s+(DEFAULT|MATERIALIZED|ALIAS)\\s+([^,]+))?(?:\\s+COMMENT\\s+'([^']*)')?(?:\\s+CODEC\\s*\\(([^)]+)\\))?",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            ClickHouseColumnDef column = new ClickHouseColumnDef();
            column.setName(m.group(1).trim());
            column.setDataType(m.group(2).trim());
            
            if (m.group(3) != null) {
                column.setDefaultType(m.group(3).toUpperCase());
                column.setDefaultValue(m.group(4).trim());
            }
            
            if (m.group(5) != null) {
                column.setComment(m.group(5));
            }
            
            if (m.group(6) != null) {
                column.setCodec(m.group(6));
            }
            
            // 检测 Nullable
            column.setNullable(colDef.toUpperCase().contains("NULLABLE"));
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析引擎
     */
    private void parseEngine(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "ENGINE\\s*=\\s*(\\w+)(?:\\s*\\(([^)]*)\\))?",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setEngine(m.group(1));
            if (m.group(2) != null) {
                String[] params = m.group(2).split("\\s*,\\s*");
                createTable.setEngineParams(Arrays.asList(params));
            }
        }
    }
    
    /**
     * 解析 PARTITION BY
     */
    private void parsePartitionBy(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PARTITION\\s+BY\\s+(.+?)(?:\\s*(?:ORDER|PRIMARY|SAMPLE|TTL|SETTINGS|$))",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setPartitionBy(m.group(1).trim());
        }
    }
    
    /**
     * 解析 ORDER BY
     */
    private void parseOrderBy(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "ORDER\\s+BY\\s+(.+?)(?:\\s*(?:PRIMARY|SAMPLE|TTL|SETTINGS|$))",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setOrderBy(m.group(1).trim());
        }
    }
    
    /**
     * 解析 PRIMARY KEY
     */
    private void parsePrimaryKey(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PRIMARY\\s+KEY\\s+(.+?)(?:\\s*(?:SAMPLE|TTL|SETTINGS|$))",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setPrimaryKey(m.group(1).trim());
        }
    }
    
    /**
     * 解析 SAMPLE BY
     */
    private void parseSampleBy(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "SAMPLE\\s+BY\\s+(.+?)(?:\\s*(?:TTL|SETTINGS|$))",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setSampleBy(m.group(1).trim());
        }
    }
    
    /**
     * 解析 TTL
     */
    private void parseTTL(String sql, ClickHouseCreateTable createTable) {
        Matcher m = Pattern.compile(
            "TTL\\s+(.+?)(?:\\s*(?:SETTINGS|$))",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTtl(m.group(1).trim());
        }
    }
    
    /**
     * 解析表 SETTINGS
     */
    private void parseTableSettings(String sql, ClickHouseCreateTable createTable) {
        List<ClickHouseCreateTable.Setting> settings = new ArrayList<>();
        
        Matcher m = SETTINGS_PATTERN.matcher(sql);
        if (m.find()) {
            String settingsSection = m.group(1);
            String[] settingsArr = settingsSection.split("\\s*,\\s*");
            
            for (String setting : settingsArr) {
                String[] parts = setting.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    ClickHouseCreateTable.Setting s = new ClickHouseCreateTable.Setting();
                    s.setKey(parts[0].trim());
                    s.setValue(parts[1].trim());
                    settings.add(s);
                }
            }
        }
        
        createTable.setSettings(settings);
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, ClickHouseCreateTable createTable) {
        int asIndex = sql.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            String asPart = sql.substring(asIndex + 4).trim();
            if (asPart.toUpperCase().startsWith("SELECT")) {
                createTable.setAsSelect(asPart);
            }
        }
    }
    
    /**
     * 重写 parse 方法，支持 ClickHouse 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 ClickHouse 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 ClickHouse 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        ClickHouseSelectDetails chDetails = new ClickHouseSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            chDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            chDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            chDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 ARRAY JOIN
        List<ClickHouseSelectDetails.ArrayJoin> arrayJoins = new ArrayList<>();
        Matcher arrayJoinMatcher = ARRAY_JOIN_PATTERN.matcher(sql);
        while (arrayJoinMatcher.find()) {
            ClickHouseSelectDetails.ArrayJoin aj = new ClickHouseSelectDetails.ArrayJoin();
            aj.setArrayExpression(arrayJoinMatcher.group(2));
            if (arrayJoinMatcher.group(3) != null) {
                aj.setAlias(arrayJoinMatcher.group(3));
            }
            aj.setLeft(arrayJoinMatcher.group(1) != null);
            arrayJoins.add(aj);
        }
        chDetails.setArrayJoins(arrayJoins);
        
        // 检测 SAMPLE
        Matcher sampleMatcher = SAMPLE_PATTERN.matcher(sql);
        if (sampleMatcher.find()) {
            ClickHouseSelectDetails.Sample sample = new ClickHouseSelectDetails.Sample();
            if (sampleMatcher.group(1) != null) {
                // SAMPLE 0.1 [OFFSET 0.5]
                sample.setRatio(Double.parseDouble(sampleMatcher.group(1)));
                if (sampleMatcher.group(2) != null) {
                    sample.setOffset((long)(Double.parseDouble(sampleMatcher.group(2)) * 1000000000));
                }
            } else if (sampleMatcher.group(3) != null) {
                // SAMPLE 1000000 [OFFSET 500000]
                sample.setN(Long.parseLong(sampleMatcher.group(3)));
                if (sampleMatcher.group(4) != null) {
                    sample.setM(Long.parseLong(sampleMatcher.group(4)));
                }
            }
            chDetails.setSample(sample);
        }
        
        // 检测 FINAL
        chDetails.setFinalModifier(upper.contains("FROM") && 
            Pattern.compile("\\bFINAL\\b", Pattern.CASE_INSENSITIVE).matcher(upper).find());
        
        // 检测 PREWHERE
        Matcher prewhereMatcher = PREWHERE_PATTERN.matcher(sql);
        if (prewhereMatcher.find()) {
            chDetails.setPrewhere(prewhereMatcher.group(1).trim());
        }
        
        // 检测 LIMIT n, m 语法
        Matcher limitMatcher = LIMIT_N_M_PATTERN.matcher(sql);
        if (limitMatcher.find()) {
            if (limitMatcher.group(1) != null) {
                chDetails.setLimitOffset(Long.parseLong(limitMatcher.group(1)));
            }
            chDetails.setLimitCount(Long.parseLong(limitMatcher.group(2)));
            chDetails.setWithTies(upper.contains("WITH TIES"));
        }
        
        // 检测 GROUP BY WITH ROLLUP / CUBE / TOTALS
        if (upper.contains("WITH ROLLUP")) {
            chDetails.setWithRollup(true);
        }
        if (upper.contains("WITH CUBE")) {
            chDetails.setWithCube(true);
        }
        if (upper.contains("WITH TOTALS")) {
            chDetails.setWithTotals(true);
        }
        
        // 检测 FORMAT
        Matcher formatMatcher = FORMAT_PATTERN.matcher(sql);
        if (formatMatcher.find()) {
            chDetails.setFormat(formatMatcher.group(1));
        }
        
        // 检测 SETTINGS
        List<ClickHouseSelectDetails.Setting> settings = new ArrayList<>();
        Matcher settingsMatcher = SETTINGS_PATTERN.matcher(sql);
        if (settingsMatcher.find()) {
            String settingsSection = settingsMatcher.group(1);
            String[] settingsArr = settingsSection.split("\\s*,\\s*");
            
            for (String setting : settingsArr) {
                String[] parts = setting.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    ClickHouseSelectDetails.Setting s = new ClickHouseSelectDetails.Setting();
                    s.setKey(parts[0].trim());
                    s.setValue(parts[1].trim());
                    settings.add(s);
                }
            }
        }
        chDetails.setSettings(settings);
        
        stmt.setSelectDetails(chDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 ClickHouse 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        ClickHouseInsertDetails chDetails = new ClickHouseInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), chDetails);
        }
        
        // 检测 FORMAT
        Matcher formatMatcher = FORMAT_PATTERN.matcher(sql);
        if (formatMatcher.find()) {
            chDetails.setFormat(formatMatcher.group(1));
        }
        
        // 检测 SETTINGS
        List<ClickHouseInsertDetails.Setting> settings = new ArrayList<>();
        Matcher settingsMatcher = SETTINGS_PATTERN.matcher(sql);
        if (settingsMatcher.find()) {
            String settingsSection = settingsMatcher.group(1);
            String[] settingsArr = settingsSection.split("\\s*,\\s*");
            
            for (String setting : settingsArr) {
                String[] parts = setting.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    ClickHouseInsertDetails.Setting s = new ClickHouseInsertDetails.Setting();
                    s.setKey(parts[0].trim());
                    s.setValue(parts[1].trim());
                    settings.add(s);
                }
            }
        }
        chDetails.setSettings(settings);
        
        stmt.setInsertDetails(chDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, ClickHouseInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
