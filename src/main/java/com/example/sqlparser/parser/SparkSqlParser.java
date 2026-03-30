package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.spark.*;

import java.util.*;
import java.util.regex.*;

/**
 * Spark SQL 解析器
 * 扩展基础 SQL 解析器，支持 Spark 特有语法
 */
public class SparkSqlParser extends SqlParser {
    
    // Spark 特有的正则模式
    private static final Pattern LATERAL_VIEW_PATTERN = Pattern.compile(
        "LATERAL\\s+VIEW\\s+(?:OUTER\\s+)?(\\w+)\\s*\\(([^)]+)\\)\\s+(\\w+)(?:\\s+AS\\s+([^)]+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DISTRIBUTE_BY_PATTERN = Pattern.compile(
        "DISTRIBUTE\\s+BY\\s+([^)]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CLUSTER_BY_PATTERN = Pattern.compile(
        "CLUSTER\\s+BY\\s+([^)]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SORT_BY_PATTERN = Pattern.compile(
        "SORT\\s+BY\\s+([^)]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HINT_PATTERN = Pattern.compile(
        "/\\*\\+\\s*([^\\+]+)\\s*\\+\\*/",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SAMPLE_PATTERN = Pattern.compile(
        "TABLESAMPLE\\s*\\(\\s*(?:(\\d+(?:\\.\\d+)?)\\s*PERCENT|\\d+\\s*ROWS)(?:\\s+REPEATABLE\\s*\\(\\s*(\\d+)\\s*\\))?\\s*\\)",
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
    
    /**
     * 解析 Spark CREATE TABLE
     */
    public SparkCreateTable parseCreateTable(String sql) {
        SparkCreateTable createTable = new SparkCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测临时表
        createTable.setTemporary(upper.contains("CREATE TEMPORARY"));
        createTable.setGlobalTemporary(upper.contains("CREATE GLOBAL TEMPORARY"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 检测 OR REPLACE
        createTable.setOrReplace(upper.contains("OR REPLACE"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析 USING
        parseUsing(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析分区
        parsePartitionedBy(sql, createTable);
        
        // 解析分桶
        parseClusteredBy(sql, createTable);
        
        // 解析排序
        parseSortedBy(sql, createTable);
        
        // 解析 OPTIONS
        parseOptions(sql, createTable);
        
        // 解析 LOCATION
        parseLocation(sql, createTable);
        
        // 解析 COMMENT
        parseComment(sql, createTable);
        
        // 解析 TBLPROPERTIES
        parseTblProperties(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, SparkCreateTable createTable) {
        // 尝试匹配 schema.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:GLOBAL\\s+TEMPORARY\\s+|TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setSchema(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:OR\\s+REPLACE\\s+)?(?:GLOBAL\\s+TEMPORARY\\s+|TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析 USING
     */
    private void parseUsing(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
            "USING\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setUsing(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<SparkColumnDef> parseColumns(String sql) {
        List<SparkColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+[\\w<>,()\\s]+(?:\\s+NOT\\s+NULL)?(?:\\s+COMMENT\\s+'[^']*')?,?\\s*)+)\\s*\\)(?:\\s*(?:USING|PARTITIONED|CLUSTERED|SORTED|OPTIONS|LOCATION|COMMENT|TBLPROPERTIES|AS|$))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            String[] colDefs = colSection.split("\\s*,\\s*(?=\\w+\\s+\\w)");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                
                SparkColumnDef column = parseColumnDef(colDef);
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
    private SparkColumnDef parseColumnDef(String colDef) {
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w<>,()\\s]+?)(?:\\s+(NOT\\s+NULL))?(?:\\s+COMMENT\\s+'([^']*)')?\\s*$",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            SparkColumnDef column = new SparkColumnDef();
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
     * 解析 PARTITIONED BY
     */
    private void parsePartitionedBy(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PARTITIONED\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
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
     * 解析 CLUSTERED BY
     */
    private void parseClusteredBy(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
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
     * 解析 SORTED BY
     */
    private void parseSortedBy(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
            "SORTED\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setSortedBy(Arrays.asList(cols));
        }
    }
    
    /**
     * 解析 OPTIONS
     */
    private void parseOptions(String sql, SparkCreateTable createTable) {
        List<SparkCreateTable.Option> options = new ArrayList<>();
        
        Matcher m = Pattern.compile(
            "OPTIONS\\s*\\(\\s*((?:'[^']+'\\s*\\s*[^,]+,?\\s*)+)\\s*\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String optsSection = m.group(1);
            String[] opts = optsSection.split("\\s*,\\s*");
            
            for (String opt : opts) {
                String[] parts = opt.split("\\s*\\s*", 2);
                if (parts.length == 2) {
                    SparkCreateTable.Option option = new SparkCreateTable.Option();
                    option.setKey(parts[0].trim().replace("'", ""));
                    option.setValue(parts[1].trim());
                    options.add(option);
                }
            }
        }
        
        createTable.setOptions(options);
    }
    
    /**
     * 解析 LOCATION
     */
    private void parseLocation(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
            "LOCATION\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setLocation(m.group(1));
        }
    }
    
    /**
     * 解析 COMMENT
     */
    private void parseComment(String sql, SparkCreateTable createTable) {
        Matcher m = Pattern.compile(
            "COMMENT\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setComment(m.group(1));
        }
    }
    
    /**
     * 解析 TBLPROPERTIES
     */
    private void parseTblProperties(String sql, SparkCreateTable createTable) {
        List<SparkCreateTable.TblProperty> properties = new ArrayList<>();
        
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
                    SparkCreateTable.TblProperty property = new SparkCreateTable.TblProperty();
                    property.setKey(parts[0].trim().replace("'", ""));
                    property.setValue(parts[1].trim().replace("'", ""));
                    properties.add(property);
                }
            }
        }
        
        createTable.setTblProperties(properties);
    }
    
    /**
     * 解析 AS SELECT
     */
    private void parseAsSelect(String sql, SparkCreateTable createTable) {
        int asIndex = sql.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            String asPart = sql.substring(asIndex + 4).trim();
            if (asPart.toUpperCase().startsWith("SELECT")) {
                createTable.setAsSelect(asPart);
            }
        }
    }
    
    /**
     * 重写 parse 方法，支持 Spark 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 Spark 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 Spark 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        SparkSelectDetails sparkDetails = new SparkSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            sparkDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            sparkDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            sparkDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 LATERAL VIEW
        List<SparkSelectDetails.LateralView> lateralViews = new ArrayList<>();
        Matcher lateralMatcher = LATERAL_VIEW_PATTERN.matcher(sql);
        while (lateralMatcher.find()) {
            SparkSelectDetails.LateralView lv = new SparkSelectDetails.LateralView();
            lv.setExpression(lateralMatcher.group(1) + "(" + lateralMatcher.group(2) + ")");
            lv.setTableAlias(lateralMatcher.group(3));
            if (lateralMatcher.group(4) != null) {
                lv.setColumnAliases(Arrays.asList(lateralMatcher.group(4).split("\\s*,\\s*")));
            }
            lv.setOuter(upper.substring(lateralMatcher.start(), lateralMatcher.end()).contains("OUTER"));
            lateralViews.add(lv);
        }
        sparkDetails.setLateralViews(lateralViews);
        
        // 检测 DISTRIBUTE BY
        Matcher distMatcher = DISTRIBUTE_BY_PATTERN.matcher(sql);
        if (distMatcher.find()) {
            String[] cols = distMatcher.group(1).split("\\s*,\\s*");
            sparkDetails.setDistributeBy(Arrays.asList(cols));
        }
        
        // 检测 CLUSTER BY
        Matcher clusterMatcher = CLUSTER_BY_PATTERN.matcher(sql);
        if (clusterMatcher.find()) {
            String[] cols = clusterMatcher.group(1).split("\\s*,\\s*");
            sparkDetails.setClusterBy(Arrays.asList(cols));
        }
        
        // 检测 SORT BY
        Matcher sortMatcher = SORT_BY_PATTERN.matcher(sql);
        if (sortMatcher.find()) {
            String[] cols = sortMatcher.group(1).split("\\s*,\\s*");
            sparkDetails.setSortBy(Arrays.asList(cols));
        }
        
        // 检测 Hints
        List<SparkSelectDetails.Hint> hints = new ArrayList<>();
        Matcher hintMatcher = HINT_PATTERN.matcher(sql);
        while (hintMatcher.find()) {
            SparkSelectDetails.Hint hint = new SparkSelectDetails.Hint();
            hint.setName(hintMatcher.group(1).trim());
            hints.add(hint);
        }
        sparkDetails.setHints(hints);
        
        // 检测 TABLESAMPLE
        Matcher sampleMatcher = SAMPLE_PATTERN.matcher(sql);
        if (sampleMatcher.find()) {
            SparkSelectDetails.Sample sample = new SparkSelectDetails.Sample();
            if (sampleMatcher.group(1) != null) {
                sample.setPercentage(Double.parseDouble(sampleMatcher.group(1)));
            }
            if (sampleMatcher.group(2) != null) {
                sample.setSeed(Long.parseLong(sampleMatcher.group(2)));
            }
            sparkDetails.setSample(sample);
        }
        
        stmt.setSelectDetails(sparkDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 Spark 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        SparkInsertDetails sparkDetails = new SparkInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), sparkDetails);
        }
        
        // 检测 OVERWRITE / INTO
        Matcher m = INSERT_OVERWRITE_PATTERN.matcher(sql);
        if (m.find()) {
            sparkDetails.setOverwrite("OVERWRITE".equalsIgnoreCase(m.group(1)));
            sparkDetails.setInto("INTO".equalsIgnoreCase(m.group(1)));
        }
        
        // 检测 TABLE 关键字
        sparkDetails.setTableKeyword(upper.contains("INSERT INTO TABLE") || upper.contains("INSERT OVERWRITE TABLE"));
        
        // 解析分区指定
        List<SparkInsertDetails.PartitionSpec> partitionSpecs = new ArrayList<>();
        Matcher partitionMatcher = PARTITION_SPEC_PATTERN.matcher(sql);
        if (partitionMatcher.find()) {
            String[] parts = partitionMatcher.group(1).split("\\s*,\\s*");
            for (String part : parts) {
                String[] kv = part.split("\\s*=\\s*", 2);
                SparkInsertDetails.PartitionSpec spec = new SparkInsertDetails.PartitionSpec();
                spec.setColumn(kv[0].trim());
                if (kv.length > 1) {
                    spec.setValue(kv[1].trim());
                }
                partitionSpecs.add(spec);
            }
        }
        sparkDetails.setPartitionSpecs(partitionSpecs);
        
        // 检测 IF NOT EXISTS
        sparkDetails.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        stmt.setInsertDetails(sparkDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, SparkInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
