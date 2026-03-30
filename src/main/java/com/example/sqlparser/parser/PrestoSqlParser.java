package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.presto.*;

import java.util.*;
import java.util.regex.*;

/**
 * Presto SQL 解析器
 * 扩展基础 SQL 解析器，支持 Presto 特有语法
 */
public class PrestoSqlParser extends SqlParser {
    
    // Presto 特有的正则模式
    private static final Pattern CATALOG_SCHEMA_TABLE_PATTERN = Pattern.compile(
        "(\\w+)\\s*\\.\\s*(\\w+)\\s*\\.\\s*(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UNNEST_PATTERN = Pattern.compile(
        "UNNEST\\s*\\(([^)]+)\\)(?:\\s+AS\\s+(\\w+))?(?:\\s+WITH\\s+ORDINALITY)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABLESAMPLE_PATTERN = Pattern.compile(
        "TABLESAMPLE\\s+(BERNOULLI|SYSTEM)\\s*\\(\\s*(\\d+(?:\\.\\d+)?)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern WITH_RECURSIVE_PATTERN = Pattern.compile(
        "WITH\\s+RECURSIVE",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern WINDOW_FUNCTION_PATTERN = Pattern.compile(
        "(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s+OVER\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LIMIT_ALL_PATTERN = Pattern.compile(
        "LIMIT\\s+ALL",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FETCH_FIRST_PATTERN = Pattern.compile(
        "FETCH\\s+FIRST\\s+(\\d+)\\s+ROW(?:S)?(?:\\s+WITH\\s+TIES)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern OFFSET_PATTERN = Pattern.compile(
        "OFFSET\\s+(\\d+)(?:\\s+ROW(?:S)?)?",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 Presto CREATE TABLE
     */
    public PrestoCreateTable parseCreateTable(String sql) {
        PrestoCreateTable createTable = new PrestoCreateTable();
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(sql.toUpperCase().contains("IF NOT EXISTS"));
        
        // 解析表名（支持 catalog.schema.table 格式）
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析 WITH 属性
        createTable.setProperties(parseProperties(sql));
        
        // 解析 COMMENT
        parseComment(sql, createTable);
        
        // 解析 PARTITIONED BY
        parsePartitionedBy(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, PrestoCreateTable createTable) {
        // 尝试匹配 catalog.schema.table
        Matcher m = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setCatalog(m.group(1));
            createTable.setSchema(m.group(2));
            createTable.setTableName(m.group(3));
            return;
        }
        
        // 尝试匹配 schema.table
        m = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setSchema(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<PrestoColumnDef> parseColumns(String sql) {
        List<PrestoColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+[\\w<>,()\\s]+(?:\\s+NOT\\s+NULL)?(?:\\s+COMMENT\\s+'[^']*')?,?\\s*)+)\\s*\\)(?:\\s*(?:WITH|PARTITIONED|AS|COMMENT|$))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            String[] colDefs = colSection.split("\\s*,\\s*(?=\\w+\\s+\\w)");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty() || colDef.toUpperCase().startsWith("PRIMARY KEY") 
                    || colDef.toUpperCase().startsWith("CONSTRAINT")) {
                    continue;
                }
                
                PrestoColumnDef column = parseColumnDef(colDef);
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
    private PrestoColumnDef parseColumnDef(String colDef) {
        // 格式: column_name data_type [NOT NULL] [COMMENT 'comment']
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w<>,()\\s]+?)(?:\\s+(NOT\\s+NULL))?(?:\\s+COMMENT\\s+'([^']*)')?\\s*$",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            PrestoColumnDef column = new PrestoColumnDef();
            column.setName(m.group(1).trim());
            column.setDataType(m.group(2).trim());
            
            if (m.group(3) != null) {
                column.setNullable(false);
            } else {
                column.setNullable(true);
            }
            
            if (m.group(4) != null) {
                column.setComment(m.group(4));
            }
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析 WITH 属性
     */
    private List<PrestoCreateTable.Property> parseProperties(String sql) {
        List<PrestoCreateTable.Property> properties = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "WITH\\s*\\(\\s*((?:\\w+\\s*=\\s*[^,]+,?\\s*)+)\\s*\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = pattern.matcher(sql);
        if (m.find()) {
            String propsSection = m.group(1);
            String[] props = propsSection.split("\\s*,\\s*");
            
            for (String prop : props) {
                String[] parts = prop.split("\\s*=\\s*", 2);
                if (parts.length == 2) {
                    PrestoCreateTable.Property property = new PrestoCreateTable.Property();
                    property.setKey(parts[0].trim());
                    property.setValue(parts[1].trim());
                    properties.add(property);
                }
            }
        }
        
        return properties;
    }
    
    /**
     * 解析 COMMENT
     */
    private void parseComment(String sql, PrestoCreateTable createTable) {
        Pattern pattern = Pattern.compile(
            "COMMENT\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(sql);
        if (m.find()) {
            createTable.setComment(m.group(1));
        }
    }
    
    /**
     * 解析 PARTITIONED BY
     */
    private void parsePartitionedBy(String sql, PrestoCreateTable createTable) {
        Pattern pattern = Pattern.compile(
            "PARTITIONED\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(sql);
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            List<String> partitionedBy = new ArrayList<>();
            for (String col : cols) {
                partitionedBy.add(col.trim());
            }
            createTable.setPartitionedBy(partitionedBy);
        }
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, PrestoCreateTable createTable) {
        int asIndex = sql.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            String asPart = sql.substring(asIndex + 4).trim();
            if (asPart.toUpperCase().startsWith("SELECT")) {
                createTable.setAsSelect(asPart);
                createTable.setWithData(true);
            }
        }
        
        // 检测 WITH NO DATA
        if (sql.toUpperCase().contains("WITH NO DATA")) {
            createTable.setWithData(false);
        }
    }
    
    /**
     * 重写 parse 方法，支持 Presto 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 检测 EXPLAIN
        if (upper.startsWith("EXPLAIN")) {
            return parseExplain(sql);
        }
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 Presto 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 解析 EXPLAIN 语句
     */
    private SqlStatement parseExplain(String sql) {
        SqlStatement stmt = new SqlStatement();
        stmt.setType(StatementType.SELECT);  // EXPLAIN 返回查询计划
        
        PrestoExplain explain = new PrestoExplain();
        
        String upper = sql.toUpperCase();
        if (upper.contains("EXPLAIN (TYPE DISTRIBUTED")) {
            explain.setType(PrestoExplain.ExplainType.DISTRIBUTED);
        } else if (upper.contains("EXPLAIN (TYPE IO")) {
            explain.setType(PrestoExplain.ExplainType.IO);
        } else if (upper.contains("EXPLAIN (TYPE VALIDATE")) {
            explain.setType(PrestoExplain.ExplainType.VALIDATE);
        } else {
            explain.setType(PrestoExplain.ExplainType.LOGICAL);
        }
        
        // 提取实际查询语句
        int selectIndex = upper.indexOf("SELECT");
        if (selectIndex > 0) {
            explain.setStatement(sql.substring(selectIndex));
        }
        
        // 这里可以将 explain 对象附加到 stmt，需要扩展模型
        
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 Presto 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PrestoSelectDetails prestoDetails = new PrestoSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            prestoDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            prestoDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            prestoDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 WITH RECURSIVE
        if (WITH_RECURSIVE_PATTERN.matcher(sql).find()) {
            prestoDetails.setRecursive(true);
            prestoDetails.setRecursiveCtes(parseRecursiveCtes(sql));
        }
        
        // 检测 UNNEST
        Matcher unnestMatcher = UNNEST_PATTERN.matcher(sql);
        List<PrestoSelectDetails.UnnestClause> unnestClauses = new ArrayList<>();
        while (unnestMatcher.find()) {
            PrestoSelectDetails.UnnestClause unnest = new PrestoSelectDetails.UnnestClause();
            String[] arrays = unnestMatcher.group(1).split("\\s*,\\s*");
            unnest.setArrays(Arrays.asList(arrays));
            if (unnestMatcher.group(2) != null) {
                unnest.setAlias(unnestMatcher.group(2));
            }
            unnest.setWithOrdinality(upper.contains("WITH ORDINALITY"));
            unnestClauses.add(unnest);
        }
        prestoDetails.setUnnestClauses(unnestClauses);
        
        // 检测 TABLESAMPLE
        Matcher sampleMatcher = TABLESAMPLE_PATTERN.matcher(sql);
        if (sampleMatcher.find()) {
            PrestoSelectDetails.TableSample sample = new PrestoSelectDetails.TableSample();
            sample.setType(sampleMatcher.group(1).toUpperCase());
            sample.setPercentage(Double.parseDouble(sampleMatcher.group(2)));
            prestoDetails.setTableSample(sample);
        }
        
        // 检测 LIMIT ALL
        if (LIMIT_ALL_PATTERN.matcher(sql).find()) {
            prestoDetails.setLimitAll(true);
        }
        
        // 检测 FETCH FIRST
        Matcher fetchMatcher = FETCH_FIRST_PATTERN.matcher(sql);
        if (fetchMatcher.find()) {
            PrestoSelectDetails.FetchFirst fetch = new PrestoSelectDetails.FetchFirst();
            fetch.setCount(Long.parseLong(fetchMatcher.group(1)));
            fetch.setWithTies(upper.contains("WITH TIES"));
            prestoDetails.setFetchFirst(fetch);
        }
        
        // 检测 OFFSET
        Matcher offsetMatcher = OFFSET_PATTERN.matcher(sql);
        if (offsetMatcher.find()) {
            prestoDetails.setOffset(Long.parseLong(offsetMatcher.group(1)));
        }
        
        // 检测窗口函数
        prestoDetails.setWindowFunctions(parseWindowFunctions(sql));
        
        stmt.setSelectDetails(prestoDetails);
    }
    
    /**
     * 解析递归 CTE
     */
    private List<PrestoSelectDetails.RecursiveCte> parseRecursiveCtes(String sql) {
        List<PrestoSelectDetails.RecursiveCte> ctes = new ArrayList<>();
        // 简化实现，实际解析较复杂
        return ctes;
    }
    
    /**
     * 解析窗口函数
     */
    private List<PrestoSelectDetails.WindowFunction> parseWindowFunctions(String sql) {
        List<PrestoSelectDetails.WindowFunction> functions = new ArrayList<>();
        
        Matcher m = WINDOW_FUNCTION_PATTERN.matcher(sql);
        while (m.find()) {
            PrestoSelectDetails.WindowFunction func = new PrestoSelectDetails.WindowFunction();
            func.setFunction(m.group(1));
            func.setPartitionBy(m.group(2));
            
            String overClause = m.group(3);
            // 解析 PARTITION BY 和 ORDER BY
            if (overClause.toUpperCase().contains("PARTITION BY")) {
                Pattern partitionPattern = Pattern.compile("PARTITION\\s+BY\\s+([^)]+)", Pattern.CASE_INSENSITIVE);
                Matcher pm = partitionPattern.matcher(overClause);
                if (pm.find()) {
                    func.setPartitionBy(pm.group(1).trim());
                }
            }
            if (overClause.toUpperCase().contains("ORDER BY")) {
                Pattern orderPattern = Pattern.compile("ORDER\\s+BY\\s+([^)]+)", Pattern.CASE_INSENSITIVE);
                Matcher om = orderPattern.matcher(overClause);
                if (om.find()) {
                    func.setOrderBy(om.group(1).trim());
                }
            }
            
            functions.add(func);
        }
        
        return functions;
    }
    
    /**
     * 扩展 INSERT 详情为 Presto 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        PrestoInsertDetails prestoDetails = new PrestoInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), prestoDetails);
        }
        
        // 检测 OVERWRITE
        prestoDetails.setOverwrite(upper.contains("OVERWRITE"));
        
        // 检测 IF NOT EXISTS
        prestoDetails.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        stmt.setInsertDetails(prestoDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, PrestoInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
