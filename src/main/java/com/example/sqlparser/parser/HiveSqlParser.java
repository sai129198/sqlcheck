package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.hive.*;

import java.util.*;
import java.util.regex.*;

/**
 * Hive SQL 解析器
 * 扩展基础 SQL 解析器，支持 Hive 特有语法
 */
public class HiveSqlParser extends SqlParser {
    
    // Hive 特有的正则模式
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
    
    private static final Pattern INSERT_OVERWRITE_PATTERN = Pattern.compile(
        "INSERT\\s+(OVERWRITE|INTO)\\s+(?:TABLE\\s+)?(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PARTITION_SPEC_PATTERN = Pattern.compile(
        "PARTITION\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 Hive CREATE TABLE
     */
    public HiveCreateTable parseCreateTable(String sql) {
        HiveCreateTable createTable = new HiveCreateTable();
        
        String upper = sql.toUpperCase();
        
        // 检测 EXTERNAL
        createTable.setExternal(upper.contains("CREATE EXTERNAL"));
        
        // 检测 TEMPORARY
        createTable.setTemporary(upper.contains("CREATE TEMPORARY"));
        
        // 检测 IF NOT EXISTS
        createTable.setIfNotExists(upper.contains("IF NOT EXISTS"));
        
        // 解析表名
        parseTableName(sql, createTable);
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析分区列
        parsePartitionColumns(sql, createTable);
        
        // 解析分桶
        parseClusteredBy(sql, createTable);
        
        // 解析行格式
        parseRowFormat(sql, createTable);
        
        // 解析存储格式
        parseStoredAs(sql, createTable);
        
        // 解析位置
        parseLocation(sql, createTable);
        
        // 解析表属性
        parseProperties(sql, createTable);
        
        // 解析注释
        parseComment(sql, createTable);
        
        // 解析 AS SELECT
        parseAsSelect(sql, createTable);
        
        // 解析 LIKE
        parseLike(sql, createTable);
        
        return createTable;
    }
    
    /**
     * 解析表名
     */
    private void parseTableName(String sql, HiveCreateTable createTable) {
        // 尝试匹配 database.table
        Matcher m = Pattern.compile(
            "CREATE\\s+(?:EXTERNAL\\s+|TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\.\\s*(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setDatabase(m.group(1));
            createTable.setTableName(m.group(2));
            return;
        }
        
        // 简单表名
        m = Pattern.compile(
            "CREATE\\s+(?:EXTERNAL\\s+|TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setTableName(m.group(1));
        }
    }
    
    /**
     * 解析列定义
     */
    private List<HiveColumnDef> parseColumns(String sql) {
        List<HiveColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分（在第一个括号内，排除分区定义）
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+[\\w<>,()\\s]+(?:\\s+COMMENT\\s+'[^']*')?,?\\s*)+)\\s*\\)(?:\\s*(?:PARTITIONED|CLUSTERED|SORTED|ROW|STORED|LOCATION|TBLPROPERTIES|AS|COMMENT|$))",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            String[] colDefs = colSection.split("\\s*,\\s*(?=\\w+\\s+\\w)");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                
                HiveColumnDef column = parseColumnDef(colDef);
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
    private HiveColumnDef parseColumnDef(String colDef) {
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+([\\w<>,()\\s]+?)(?:\\s+COMMENT\\s+'([^']*)')?\\s*$",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef.trim());
        if (m.find()) {
            HiveColumnDef column = new HiveColumnDef();
            column.setName(m.group(1).trim());
            column.setDataType(m.group(2).trim());
            
            if (m.group(3) != null) {
                column.setComment(m.group(3));
            }
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析分区列
     */
    private void parsePartitionColumns(String sql, HiveCreateTable createTable) {
        Matcher m = Pattern.compile(
            "PARTITIONED\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] colDefs = m.group(1).split("\\s*,\\s*");
            List<HiveColumnDef> partitionColumns = new ArrayList<>();
            
            for (String colDef : colDefs) {
                HiveColumnDef column = parseColumnDef(colDef);
                if (column != null) {
                    partitionColumns.add(column);
                }
            }
            
            createTable.setPartitionColumns(partitionColumns);
        }
    }
    
    /**
     * 解析分桶
     */
    private void parseClusteredBy(String sql, HiveCreateTable createTable) {
        Matcher m = Pattern.compile(
            "CLUSTERED\\s+BY\\s*\\(([^)]+)\\)\\s+INTO\\s+(\\d+)\\s+BUCKETS",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setClusteredBy(Arrays.asList(cols));
            createTable.setNumBuckets(Integer.parseInt(m.group(2)));
        }
        
        // 解析 SORTED BY
        m = Pattern.compile(
            "SORTED\\s+BY\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            String[] cols = m.group(1).split("\\s*,\\s*");
            createTable.setSortedBy(Arrays.asList(cols));
        }
    }
    
    /**
     * 解析行格式
     */
    private void parseRowFormat(String sql, HiveCreateTable createTable) {
        Matcher m = Pattern.compile(
            "ROW\\s+FORMAT\\s+DELIMITED" +
            "(?:\\s+FIELDS\\s+TERMINATED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+COLLECTION\\s+ITEMS\\s+TERMINATED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+MAP\\s+KEYS\\s+TERMINATED\\s+BY\\s+'([^']+)')?" +
            "(?:\\s+LINES\\s+TERMINATED\\s+BY\\s+'([^']+)')?",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setRowFormat("DELIMITED");
            if (m.group(1) != null) createTable.setFieldDelim(m.group(1));
            if (m.group(2) != null) createTable.setCollectionDelim(m.group(2));
            if (m.group(3) != null) createTable.setMapKeyDelim(m.group(3));
            if (m.group(4) != null) createTable.setLineDelim(m.group(4));
        }
        
        // SERDE 格式
        m = Pattern.compile(
            "ROW\\s+FORMAT\\s+SERDE\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setRowFormat("SERDE: " + m.group(1));
        }
    }
    
    /**
     * 解析存储格式
     */
    private void parseStoredAs(String sql, HiveCreateTable createTable) {
        // STORED AS
        Matcher m = Pattern.compile(
            "STORED\\s+AS\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setStoredAs(m.group(1));
        }
        
        // INPUTFORMAT/OUTPUTFORMAT
        m = Pattern.compile(
            "STORED\\s+AS\\s+INPUTFORMAT\\s+'([^']+)'\\s+OUTPUTFORMAT\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        ).matcher(sql);
        
        if (m.find()) {
            createTable.setInputFormat(m.group(1));
            createTable.setOutputFormat(m.group(2));
        }
    }
    
    /**
     * 解析位置
     */
    private void parseLocation(String sql, HiveCreateTable createTable) {
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
    private void parseProperties(String sql, HiveCreateTable createTable) {
        List<HiveCreateTable.Property> properties = new ArrayList<>();
        
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
                    HiveCreateTable.Property property = new HiveCreateTable.Property();
                    property.setKey(parts[0].trim().replace("'", ""));
                    property.setValue(parts[1].trim().replace("'", ""));
                    properties.add(property);
                }
            }
        }
        
        createTable.setProperties(properties);
    }
    
    /**
     * 解析注释
     */
    private void parseComment(String sql, HiveCreateTable createTable) {
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
    private void parseAsSelect(String sql, HiveCreateTable createTable) {
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
    private void parseLike(String sql, HiveCreateTable createTable) {
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
     * 重写 parse 方法，支持 Hive 特有语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 调用父类解析基础 SQL
        SqlStatement stmt = super.parse(sql);
        
        // 扩展为 Hive 特有详情
        if (stmt.isSelect()) {
            extendSelectDetails(stmt, sql);
        } else if (stmt.isInsert()) {
            extendInsertDetails(stmt, sql);
        }
        
        return stmt;
    }
    
    /**
     * 扩展 SELECT 详情为 Hive 特有
     */
    private void extendSelectDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        HiveSelectDetails hiveDetails = new HiveSelectDetails();
        
        // 复制基础信息
        if (stmt.getSelectDetails() != null) {
            hiveDetails.setQueryBlock(stmt.getSelectDetails().getQueryBlock());
            hiveDetails.setStructureType(stmt.getSelectDetails().getStructureType());
            hiveDetails.setSetOperation(stmt.getSelectDetails().getSetOperation());
        }
        
        // 检测 LATERAL VIEW
        List<HiveSelectDetails.LateralView> lateralViews = new ArrayList<>();
        Matcher lateralMatcher = LATERAL_VIEW_PATTERN.matcher(sql);
        while (lateralMatcher.find()) {
            HiveSelectDetails.LateralView lv = new HiveSelectDetails.LateralView();
            lv.setExpression(lateralMatcher.group(1) + "(" + lateralMatcher.group(2) + ")");
            lv.setTableAlias(lateralMatcher.group(3));
            if (lateralMatcher.group(4) != null) {
                lv.setColumnAliases(Arrays.asList(lateralMatcher.group(4).split("\\s*,\\s*")));
            }
            lv.setOuter(upper.substring(lateralMatcher.start(), lateralMatcher.end()).contains("OUTER"));
            lateralViews.add(lv);
        }
        hiveDetails.setLateralViews(lateralViews);
        
        // 检测 DISTRIBUTE BY
        Matcher distMatcher = DISTRIBUTE_BY_PATTERN.matcher(sql);
        if (distMatcher.find()) {
            String[] cols = distMatcher.group(1).split("\\s*,\\s*");
            hiveDetails.setDistributeBy(Arrays.asList(cols));
        }
        
        // 检测 CLUSTER BY
        Matcher clusterMatcher = CLUSTER_BY_PATTERN.matcher(sql);
        if (clusterMatcher.find()) {
            String[] cols = clusterMatcher.group(1).split("\\s*,\\s*");
            hiveDetails.setClusterBy(Arrays.asList(cols));
        }
        
        // 检测 SORT BY
        Matcher sortMatcher = SORT_BY_PATTERN.matcher(sql);
        if (sortMatcher.find()) {
            String[] cols = sortMatcher.group(1).split("\\s*,\\s*");
            hiveDetails.setSortBy(Arrays.asList(cols));
        }
        
        stmt.setSelectDetails(hiveDetails);
    }
    
    /**
     * 扩展 INSERT 详情为 Hive 特有
     */
    private void extendInsertDetails(SqlStatement stmt, String sql) {
        String upper = sql.toUpperCase();
        
        HiveInsertDetails hiveDetails = new HiveInsertDetails();
        
        // 复制基础信息
        if (stmt.getInsertDetails() != null) {
            copyInsertDetails(stmt.getInsertDetails(), hiveDetails);
        }
        
        // 检测 OVERWRITE / INTO
        Matcher m = INSERT_OVERWRITE_PATTERN.matcher(sql);
        if (m.find()) {
            hiveDetails.setOverwrite("OVERWRITE".equalsIgnoreCase(m.group(1)));
            hiveDetails.setTableKeyword(true);
        }
        
        // 解析分区指定
        List<HiveInsertDetails.PartitionSpec> partitionSpecs = new ArrayList<>();
        Matcher partitionMatcher = PARTITION_SPEC_PATTERN.matcher(sql);
        if (partitionMatcher.find()) {
            String[] parts = partitionMatcher.group(1).split("\\s*,\\s*");
            for (String part : parts) {
                String[] kv = part.split("\\s*=\\s*", 2);
                HiveInsertDetails.PartitionSpec spec = new HiveInsertDetails.PartitionSpec();
                spec.setColumn(kv[0].trim());
                if (kv.length > 1) {
                    spec.setValue(kv[1].trim());
                }
                partitionSpecs.add(spec);
            }
            // 如果有分区列但没有值，则是动态分区
            hiveDetails.setDynamicPartition(partitionSpecs.stream().anyMatch(p -> p.getValue() == null));
        }
        hiveDetails.setPartitionSpecs(partitionSpecs);
        
        // 检测 LOCAL
        hiveDetails.setLocal(upper.contains("LOCAL"));
        
        stmt.setInsertDetails(hiveDetails);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, HiveInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
