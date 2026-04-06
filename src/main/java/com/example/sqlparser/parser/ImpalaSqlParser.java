package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.impala.*;

import java.util.*;
import java.util.regex.*;

/**
 * Impala SQL 解析器
 * 扩展基础 SQL 解析器，支持 Impala 特有语法
 * 
 * 支持的 Impala 特有功能：
 * 1. CREATE TABLE (支持 STORED AS PARQUET/TEXTFILE/AVRO 等)
 * 2. COMPUTE STATS / DROP STATS
 * 3. REFRESH / INVALIDATE METADATA
 * 4. INSERT [SHUFFLE | NOSHUFFLE]
 * 5. SELECT [STRAIGHT_JOIN] [SHUFFLE | NOSHUFFLE]
 * 6. CACHED / UNCACHED
 * 7. Kudu 表支持
 */
public class ImpalaSqlParser extends SqlParser {
    
    // Impala 特有的正则模式
    private static final Pattern STRAIGHT_JOIN_PATTERN = Pattern.compile(
        "SELECT\\s+STRAIGHT_JOIN",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SHUFFLE_HINT_PATTERN = Pattern.compile(
        "\\[\\s*(SHUFFLE|NOSHUFFLE)\\s*\\]",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BROADCAST_HINT_PATTERN = Pattern.compile(
        "\\[\\s*(BROADCAST|NOBROADCAST)\\s*\\]",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSERT_OVERWRITE_PATTERN = Pattern.compile(
        "INSERT\\s+(OVERWRITE|INTO)\\s+(?:TABLE\\s+)?(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PARTITION_SPEC_PATTERN = Pattern.compile(
        "PARTITION\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMPUTE_STATS_PATTERN = Pattern.compile(
        "COMPUTE\\s+STATS\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern REFRESH_PATTERN = Pattern.compile(
        "REFRESH\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INVALIDATE_METADATA_PATTERN = Pattern.compile(
        "INVALIDATE\\s+METADATA",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CACHED_PATTERN = Pattern.compile(
        "CACHED\\s+IN\\s+'([^']+)'(?:\\s+WITH\\s+REPLICATION\\s*=\\s*(\\d+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 Impala CREATE TABLE
     */
    public ImpalaCreateTable parseCreateTable(String sql) {
        ImpalaCreateTable createTable = new ImpalaCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测 EXTERNAL
        createTable.setExternal(upper.contains("CREATE EXTERNAL"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 检测 Kudu 表
        createTable.setKuduTable(upper.contains("STORED AS KUDU") || upper.contains("PRIMARY KEY"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析分区列
        parsePartitionColumns(sql, createTable);
        
        // 解析主键（Kudu 表）
        parsePrimaryKey(sql, createTable);
        
        // 解析分桶
        parseClusteredBy(sql, createTable);
        
        // 解析行格式
        parseRowFormat(sql, createTable);
        
        // 解析存储格式
        parseStoredAs(sql, createTable);
        
        // 解析位置
        parseLocation(sql, createTable);
        
        // 解析表属性
        parseTblProperties(sql, createTable);
        
        // 解析注释
        parseComment(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        // 解析 LIKE
        parseLike(sql, createTable);
        
        // 解析缓存设置
        parseCaching(sql, createTable);
        
        // 解析 SORT BY
        parseSortBy(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, ImpalaCreateTable createTable) {
        // 尝试匹配 database.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setDatabase(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<ImpalaColumnDef> parseColumns(String sql) {
        List<ImpalaColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分 - 匹配括号内的内容，但排除 PARTITIONED BY 等后面的括号
        Pattern colPattern = Pattern.compile(
            "CREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:\\w+\\s*\\.\\s*)?\\w+\\s*\\(([^)]+(?:\\([^)]*\\)[^)]*)*)\\)(?!\\s*\\w*\\s*INTO)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            // 按逗号分割，但要处理嵌套括号的情况
            List<String> colDefs = splitColumnDefs(colSection);
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty() || colDef.toUpperCase().startsWith("PRIMARY KEY")) continue;
                
                ImpalaColumnDef column = parseColumnDef(colDef);
                if (column != null) {
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }
    
    /**
     * 分割列定义，处理嵌套括号
     */
    private List<String> splitColumnDefs(String colSection) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        
        for (int i = 0; i < colSection.length(); i++) {
            char c = colSection.charAt(i);
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                // 检查是否是列定义的分隔（后面跟着列名）
                String remaining = colSection.substring(i + 1).trim();
                if (remaining.isEmpty() || Character.isLetterOrDigit(remaining.charAt(0))) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        
        return result;
    }
    
    /**
     * 解析单个列定义
     */
    private ImpalaColumnDef parseColumnDef(String colDef) {
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w<>,()\\s]+?)(?:\\s+NOT\\s+NULL)?(?:\\s+ENCODING\\s+(\\w+))?(?:\\s+COMPRESSION\\s+(\\w+))?(?:\\s+BLOCK_SIZE\\s+(\\d+))?(?:\\s+DEFAULT\\s+([^\\s,]+))?(?:\\s+COMMENT\\s+'([^']*)')?\\s*$",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            ImpalaColumnDef column = new ImpalaColumnDef();
            column.setName(m.group(1).trim());
            column.setDataType(m.group(2).trim());
            
            if (m.group(3) != null) {
                column.setEncoding(m.group(3));
            }
            if (m.group(4) != null) {
                // 解析压缩编码
                try {
                    column.setCompressionCodec(Integer.parseInt(m.group(4)));
                } catch (NumberFormatException e) {
                    // 可能是字符串名称
                }
            }
            if (m.group(5) != null) {
                column.setBlockSize(Integer.parseInt(m.group(5)));
            }
            if (m.group(6) != null) {
                column.setDefaultValue(m.group(6));
            }
            if (m.group(7) != null) {
                column.setComment(m.group(7));
            }
            
            // 检测 NOT NULL
            column.setNullable(!colDef.toUpperCase().contains("NOT NULL"));
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析分区列
     */
    private void parsePartitionColumns(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PARTITION\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] colDefs = m.group(1).split("\\s*,\\s*");
            List<ImpalaColumnDef> partitionColumns = new ArrayList<>();
            
            for (String colDef : colDefs) {
                ImpalaColumnDef column = parseColumnDef(colDef);
                if (column != null) {
                    partitionColumns.add(column);
                }
            }
            
            createTable.setPartitionColumns(partitionColumns);
        }
    }
    
    /**
     * 解析主键（Kudu 表）
     */
    private void parsePrimaryKey(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PRIMARY\\s+KEY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setPrimaryKey(Arrays.asList(cols));
        }
    }
    
    /**
     * 解析分桶
     */
    private void parseClusteredBy(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "DISTRIBUTE\\s+BY\\s*\\(([^)]+)\\)\\s+INTO\\s+(\\d+)\\s+BUCKETS",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setDistributeBy(Arrays.asList(cols));
            createTable.setDistributeInto(Integer.parseInt(m.group(2)));
        }
        
        // CLUSTERED BY (Hive 兼容语法)
        m = Pattern.compile(
            "CLUSTERED\\s+BY\\s*\\(([^)]+)\\)\\s+INTO\\s+(\\d+)\\s+BUCKETS",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setClusteredBy(Arrays.asList(cols));
            createTable.setNumBuckets(Integer.parseInt(m.group(2)));
        }
    }
    
    /**
     * 解析行格式
     */
    private void parseRowFormat(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "ROW\\s+FORMAT\\s+DELIMITED" +
            "(?:\\s+FIELDS\\s+TERMINATED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+ESCAPED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+LINES\\s+TERMINATED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+NULL\\s+DEFINED\\s+AS\\s+'([^']+)')?",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setRowFormat("DELIMITED");
            if (m.group(1) != null) createTable.setFieldTerminator(m.group(1));
            if (m.group(2) != null) createTable.setEscapedBy(m.group(2));
            if (m.group(3) != null) createTable.setLineTerminator(m.group(3));
            if (m.group(4) != null) createTable.setNullDefinedAs(m.group(4));
        }
        
        // SERDE 格式
        m = Pattern.compile(
            "ROW\\s+FORMAT\\s+SERDE\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setSerdeClass(m.group(1));
        }
    }
    
    /**
     * 解析存储格式
     */
    private void parseStoredAs(String sql, ImpalaCreateTable createTable) {
        // STORED AS
        Matcher m = Pattern.compile(
            "STORED\\s+AS\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            try {
                createTable.setStoredAs(ImpalaFileFormat.valueOf(m.group(1).toUpperCase()));
            } catch (IllegalArgumentException e) {
                // 未知格式
            }
        }
    }
    
    /**
     * 解析位置
     */
    private void parseLocation(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "LOCATION\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setLocation(m.group(1));
        }
    }
    
    /**
     * 解析表属性
     */
    private void parseTblProperties(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "TBLPROPERTIES\\s*\\(\\s*((?:'[^']+'\\s*=\\s*'[^']*',?\\s*)+)\\s*\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String propsSection = m.group(1);
            String[] props = propsSection.split("\\s*,\\s*");
            
            for (String prop : props) {
                String[] parts = prop.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim().replace("'", "");
                    String value = parts[1].trim().replace("'", "");
                    createTable.getTblProperties().put(key, value);
                }
            }
        }
    }
    
    /**
     * 解析注释
     */
    private void parseComment(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "COMMENT\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setComment(m.group(1));
        }
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, ImpalaCreateTable createTable) {
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
    private void parseLike(String sql, ImpalaCreateTable createTable) {
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
     * 解析缓存设置
     */
    private void parseCaching(String sql, ImpalaCreateTable createTable) {
        Matcher m = CACHED_PATTERN.matcher(sql);
        if (m.find()) {
            createTable.setCached(true);
            if (m.group(2) != null) {
                createTable.setCacheReplication(Integer.parseInt(m.group(2)));
            }
        }
        
        // UNCACHED
        if (sql.toUpperCase().contains("UNCACHED")) {
            createTable.setCached(false);
        }
    }
    
    /**
     * 解析 SORT BY
     */
    private void parseSortBy(String sql, ImpalaCreateTable createTable) {
        Matcher m = Pattern.compile(
            "SORT\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setSortedBy(Arrays.asList(cols));
        }
    }
    
    /**
     * 重写 parse 方法，支持 Impala 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 检测 Impala 特有语句类型
        if (upper.startsWith("COMPUTE STATS")) {
            return parseComputeStats(sql);
        } else if (upper.startsWith("DROP STATS")) {
            return parseDropStats(sql);
        } else if (upper.startsWith("REFRESH")) {
            return parseRefresh(sql);
        } else if (upper.startsWith("INVALIDATE METADATA")) {
            return parseInvalidateMetadata(sql);
        } else if (upper.startsWith("DESCRIBE")) {
            return parseDescribe(sql);
        } else if (upper.startsWith("SHOW")) {
            return parseShow(sql);
        } else if (upper.startsWith("EXPLAIN")) {
            return parseExplain(sql);
        }
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 Impala 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 解析 COMPUTE STATS
     */
    private SqlStatement parseComputeStats(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.COMPUTE_STATS);
        
        Matcher m = COMPUTE_STATS_PATTERN.matcher(sql);
        if (m.find()) {
            metadataStmt.setDatabase(m.group(1));
            metadataStmt.setTableName(m.group(2));
        }
        
        // 解析列列表
        int parenIdx = sql.indexOf('(');
        if (parenIdx > 0) {
            int closeIdx = sql.indexOf(')', parenIdx);
            if (closeIdx > 0) {
                String cols = sql.substring(parenIdx + 1, closeIdx);
                String[] colArray = cols.split("\\s*,\\s*");
                metadataStmt.setColumns(Arrays.asList(colArray));
            }
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 DROP STATS
     */
    private SqlStatement parseDropStats(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.DROP_STATS);
        
        Matcher m = Pattern.compile(
            "DROP\\s+STATS\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            metadataStmt.setDatabase(m.group(1));
            metadataStmt.setTableName(m.group(2));
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 REFRESH
     */
    private SqlStatement parseRefresh(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.REFRESH);
        
        String upper = sql.toUpperCase();
        
        // REFRESH FUNCTIONS
        if (upper.contains("FUNCTIONS")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.REFRESH_FUNCTIONS);
            return stmt;
        }
        
        Matcher m = REFRESH_PATTERN.matcher(sql);
        if (m.find()) {
            metadataStmt.setDatabase(m.group(1));
            metadataStmt.setTableName(m.group(2));
        }
        
        // 检测 INCREMENTAL
        metadataStmt.setIncremental(upper.contains("INCREMENTAL"));
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 INVALIDATE METADATA
     */
    private SqlStatement parseInvalidateMetadata(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.INVALIDATE_METADATA);
        
        Matcher m = Pattern.compile(
            "INVALIDATE\\s+METADATA\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            metadataStmt.setDatabase(m.group(1));
            metadataStmt.setTableName(m.group(2));
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 DESCRIBE
     */
    private SqlStatement parseDescribe(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.DESCRIBE);
        
        String upper = sql.toUpperCase();
        
        // DESCRIBE DATABASE
        if (upper.contains("DATABASE")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.DESCRIBE_DATABASE);
            Matcher m = Pattern.compile(
                "DESCRIBE\\s+(?:DATABASE|SCHEMA)\\s+(?:FORMATTED\\s+)?(\\w+)",
                Pattern.CASE_INSENSITIVE
            ).matcher(sql);
            if (m.find()) {
                metadataStmt.setDatabase(m.group(1));
            }
            return stmt;
        }
        
        // FORMATTED / EXTENDED
        metadataStmt.setFormatted(upper.contains("FORMATTED"));
        metadataStmt.setExtended(upper.contains("EXTENDED"));
        
        Matcher m = Pattern.compile(
            "DESCRIBE\\s+(?:FORMATTED\\s+|EXTENDED\\s+)?(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            metadataStmt.setDatabase(m.group(1));
            metadataStmt.setTableName(m.group(2));
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 SHOW 语句
     */
    private SqlStatement parseShow(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        
        String upper = sql.toUpperCase();
        
        if (upper.contains("SHOW TABLES")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_TABLES);
            Matcher m = Pattern.compile(
                "SHOW\\s+TABLES\\s+(?:IN\\s+(\\w+))?(?:\\s+'([^']+)')?",
                Pattern.CASE_INSENSITIVE
            ).matcher(sql);
            if (m.find()) {
                metadataStmt.setInDatabase(m.group(1));
                metadataStmt.setPattern(m.group(2));
            }
        } else if (upper.contains("SHOW DATABASES")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_DATABASES);
            Matcher m = Pattern.compile(
                "SHOW\\s+DATABASES\\s+(?:LIKE\\s+'([^']+)')?",
                Pattern.CASE_INSENSITIVE
            ).matcher(sql);
            if (m.find()) {
                metadataStmt.setPattern(m.group(1));
            }
        } else if (upper.contains("SHOW CREATE TABLE")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_CREATE_TABLE);
            Matcher m = Pattern.compile(
                "SHOW\\s+CREATE\\s+TABLE\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
                Pattern.CASE_INSENSITIVE
            ).matcher(sql);
            if (m.find()) {
                metadataStmt.setDatabase(m.group(1));
                metadataStmt.setTableName(m.group(2));
            }
        } else if (upper.contains("SHOW TABLE STATS")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_TABLE_STATS);
        } else if (upper.contains("SHOW COLUMN STATS")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_COLUMN_STATS);
        } else if (upper.contains("SHOW PARTITIONS")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_PARTITIONS);
        } else if (upper.contains("SHOW FILES")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_FILES);
        } else if (upper.contains("SHOW FUNCTIONS")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_FUNCTIONS);
        } else if (upper.contains("SHOW ROLES")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_ROLES);
        } else if (upper.contains("SHOW GRANT")) {
            metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.SHOW_GRANT);
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 解析 EXPLAIN
     */
    private SqlStatement parseExplain(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.OTHER);
        
        ImpalaMetadataStmt metadataStmt = new ImpalaMetadataStmt();
        metadataStmt.setStmtType(ImpalaMetadataStmt.MetadataStmtType.EXPLAIN);
        
        String upper = sql.toUpperCase();
        
        // 解析 EXPLAIN LEVEL
        if (upper.contains("LEVEL")) {
            if (upper.contains("MINIMAL")) {
                metadataStmt.setExplainLevel(ImpalaSelectDetails.ExplainLevel.MINIMAL);
            } else if (upper.contains("STANDARD")) {
                metadataStmt.setExplainLevel(ImpalaSelectDetails.ExplainLevel.STANDARD);
            } else if (upper.contains("EXTENDED")) {
                metadataStmt.setExplainLevel(ImpalaSelectDetails.ExplainLevel.EXTENDED);
            } else if (upper.contains("VERBOSE")) {
                metadataStmt.setExplainLevel(ImpalaSelectDetails.ExplainLevel.VERBOSE);
            }
        }
        
        stmt.setMetadataStmt(metadataStmt);
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 Impala 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        ImpalaSelectDetails impalaDetails = new ImpalaSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            impalaDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            impalaDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            impalaDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 STRAIGHT_JOIN
        impalaDetails.setStraightJoin(STRAIGHT_JOIN_PATTERN.matcher(sql).find());
        
        // 检测 SHUFFLE / NOSHUFFLE
        Matcher shuffleMatcher = SHUFFLE_HINT_PATTERN.matcher(sql);
        while (shuffleMatcher.find()) {
            String hint = shuffleMatcher.group(1).toUpperCase();
            if ("SHUFFLE".equals(hint)) {
                impalaDetails.setShuffle(true);
            } else if ("NOSHUFFLE".equals(hint)) {
                impalaDetails.setNoshuffle(true);
            }
        }
        
        // 检测 BROADCAST / NOBROADCAST
        Matcher broadcastMatcher = BROADCAST_HINT_PATTERN.matcher(sql);
        while (broadcastMatcher.find()) {
            String hint = broadcastMatcher.group(1).toUpperCase();
            if ("BROADCAST".equals(hint)) {
                impalaDetails.setBroadcast(true);
            } else if ("NOBROADCAST".equals(hint)) {
                impalaDetails.setNobroadcast(true);
            }
        }
        
        stmt.setSelectDetails(impalaDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 Impala 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        ImpalaInsertDetails impalaDetails = new ImpalaInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), impalaDetails);
        }
        
        // 检测 OVERWRITE / INTO
        Matcher m = INSERT_OVERWRITE_PATTERN.matcher(sql);
        if (m.find()) {
            impalaDetails.setOverwrite("OVERWRITE".equalsIgnoreCase(m.group(1)));
            impalaDetails.setInto("INTO".equalsIgnoreCase(m.group(1)));
        }
        
        // 解析分区指定
        List<ImpalaInsertDetails.PartitionSpec> partitionSpecs = new ArrayList<>();
        Matcher partitionMatcher = PARTITION_SPEC_PATTERN.matcher(sql);
        if (partitionMatcher.find()) {
            String[] parts = partitionMatcher.group(1).split("\\s*,\\s*");
            for (String part : parts) {
                String[] kv = part.split("\\s*=\\s*", 2);
                ImpalaInsertDetails.PartitionSpec spec = new ImpalaInsertDetails.PartitionSpec();
                spec.setColumn(kv[0].trim());
                if (kv.length > 1) {
                    spec.setValue(kv[1].trim());
                }
                partitionSpecs.add(spec);
            }
        }
        impalaDetails.setPartitionSpecs(partitionSpecs);
        
        // 检测 SHUFFLE / NOSHUFFLE
        impalaDetails.setShuffle(upper.contains("[SHUFFLE]"));
        impalaDetails.setNoshuffle(upper.contains("[NOSHUFFLE]"));
        
        // 检测 CACHED
        Matcher cacheMatcher = CACHED_PATTERN.matcher(sql);
        if (cacheMatcher.find()) {
            impalaDetails.setCached(true);
            if (cacheMatcher.group(2) != null) {
                impalaDetails.setCacheReplication(Integer.parseInt(cacheMatcher.group(2)));
            }
        }
        
        // 检测 IF NOT EXISTS
        impalaDetails.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        stmt.setInsertDetails(impalaDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, ImpalaInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
