package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;
import com.example.sqlparser.model.starrocks.*;

import java.util.*;
import java.util.regex.*;

/**
 * StarRocks SQL 解析器
 * 扩展基础 SQL 解析器，支持 StarRocks 特有语法
 */
public class StarRocksSqlParser extends SqlParser {
    
    // StarRocks 特有的正则模式
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
        "CREATE\\s+(?:EXTERNAL\\s+)?TABLE\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABLE_TYPE_PATTERN = Pattern.compile(
        "(DUPLICATE|AGGREGATE|UNIQUE|PRIMARY)\\s+KEY",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PARTITION_PATTERN = Pattern.compile(
        "PARTITION\\s+BY\\s+(?:RANGE|LIST|EXPRESSION)\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DISTRIBUTED_PATTERN = Pattern.compile(
        "DISTRIBUTED\\s+BY\\s+(?:HASH|RANDOM)\\s*\\(([^)]+)\\)\\s*BUCKETS\\s*(\\d+|AUTO)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PROPERTIES_PATTERN = Pattern.compile(
        "\\(\\s*\"([^\"]+)\"\\s*=\\s*\"([^\"]+)\"\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSERT_OVERWRITE_PATTERN = Pattern.compile(
        "INSERT\\s+(INTO|OVERWRITE)\\s+(?:(\\w+)\\s*\\.\\s*)?(\\w+)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PARTITION_SPEC_PATTERN = Pattern.compile(
        "PARTITION\\s*\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 StarRocks CREATE TABLE 语句
     */
    public StarRocksCreateTable parseCreateTable(String sql) {
        StarRocksCreateTable createTable = new StarRocksCreateTable();
        
        // 解析表名
        Matcher m = CREATE_TABLE_PATTERN.matcher(sql);
        if (m.find()) {
            if (m.group(1) != null) {
                createTable.setDatabase(m.group(1));
            }
            createTable.setTableName(m.group(2));
        }
        
        // 检测 EXTERNAL TABLE
        createTable.setExternal(sql.toUpperCase().contains("EXTERNAL TABLE"));
        
        // 解析表类型
        Matcher typeMatcher = TABLE_TYPE_PATTERN.matcher(sql);
        if (typeMatcher.find()) {
            String type = typeMatcher.group(1).toUpperCase();
            switch (type) {
                case "DUPLICATE":
                    createTable.setTableType(StarRocksTableType.DUPLICATE_KEY);
                    break;
                case "AGGREGATE":
                    createTable.setTableType(StarRocksTableType.AGGREGATE_KEY);
                    break;
                case "UNIQUE":
                    createTable.setTableType(StarRocksTableType.UNIQUE_KEY);
                    break;
                case "PRIMARY":
                    createTable.setTableType(StarRocksTableType.PRIMARY_KEY);
                    break;
            }
        }
        
        // 解析列定义
        createTable.setColumns(parseColumns(sql));
        
        // 解析分区信息
        createTable.setPartitionInfo(parsePartitionInfo(sql));
        
        // 解析分桶信息
        createTable.setBucketInfo(parseBucketInfo(sql));
        
        // 解析属性
        createTable.setProperties(parseProperties(sql));
        
        return createTable;
    }
    
    /**
     * 解析列定义
     */
    private List<StarRocksColumnDef> parseColumns(String sql) {
        List<StarRocksColumnDef> columns = new ArrayList<>();
        
        // 提取列定义部分（在第一个括号内）
        Pattern colPattern = Pattern.compile(
            "\\(\\s*((?:\\w+\\s+\\w+(?:\\([^)]+\\))?[^,)]*,?\\s*)+)\\s*\\)(?:\\s*(?:DUPLICATE|AGGREGATE|UNIQUE|PRIMARY))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher m = colPattern.matcher(sql);
        if (m.find()) {
            String colSection = m.group(1);
            String[] colDefs = colSection.split("\\s*,\\s*");
            
            for (String colDef : colDefs) {
                colDef = colDef.trim();
                if (colDef.isEmpty()) continue;
                
                StarRocksColumnDef column = parseColumnDef(colDef);
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
    private StarRocksColumnDef parseColumnDef(String colDef) {
        // 格式: column_name data_type [NULL|NOT NULL] [DEFAULT value] [AGGREGATE_TYPE] [COMMENT 'comment']
        Pattern pattern = Pattern.compile(
            "(\\w+)\\s+(\\w+(?:\\([^)]+\\))?)" +
            "(?:\\s+(NULL|NOT\\s+NULL))?" +
            "(?:\\s+DEFAULT\\s+([^\\s,]+))?" +
            "(?:\\s+(SUM|MIN|MAX|REPLACE|REPLACE_IF_NOT_NULL|HLL_UNION|BITMAP_UNION|PERCENTILE_UNION))?" +
            "(?:\\s+COMMENT\\s+'([^']+)')?",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher m = pattern.matcher(colDef);
        if (m.find()) {
            StarRocksColumnDef column = new StarRocksColumnDef();
            column.setName(m.group(1));
            column.setDataType(m.group(2));
            
            if (m.group(3) != null) {
                column.setNullable(!m.group(3).toUpperCase().contains("NOT"));
            } else {
                column.setNullable(true);
            }
            
            if (m.group(4) != null) {
                column.setDefaultValue(m.group(4));
            }
            
            if (m.group(5) != null) {
                column.setAggregateType(StarRocksColumnDef.AggregateType.valueOf(m.group(5).toUpperCase()));
            }
            
            if (m.group(6) != null) {
                column.setComment(m.group(6));
            }
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 解析分区信息
     */
    private PartitionInfo parsePartitionInfo(String sql) {
        Matcher m = PARTITION_PATTERN.matcher(sql);
        if (!m.find()) {
            return null;
        }
        
        PartitionInfo info = new PartitionInfo();
        
        // 确定分区类型
        String partitionClause = sql.substring(m.start(), m.end()).toUpperCase();
        if (partitionClause.contains("RANGE")) {
            info.setType(PartitionType.RANGE);
        } else if (partitionClause.contains("LIST")) {
            info.setType(PartitionType.LIST);
        } else {
            info.setType(PartitionType.EXPRESSION);
        }
        
        // 解析分区列
        String[] cols = m.group(1).split("\\s*,\\s*");
        List<PartitionColumn> partitionColumns = new ArrayList<>();
        for (String col : cols) {
            PartitionColumn pc = new PartitionColumn();
            pc.setName(col.trim());
            partitionColumns.add(pc);
        }
        info.setPartitionColumns(partitionColumns);
        
        return info;
    }
    
    /**
     * 解析分桶信息
     */
    private BucketInfo parseBucketInfo(String sql) {
        Matcher m = DISTRIBUTED_PATTERN.matcher(sql);
        if (!m.find()) {
            return null;
        }
        
        BucketInfo info = new BucketInfo();
        
        // 解析分桶列
        String[] cols = m.group(1).split("\\s*,\\s*");
        info.setBucketColumns(Arrays.asList(cols));
        
        // 解析分桶数
        String bucketStr = m.group(2).toUpperCase();
        if ("AUTO".equals(bucketStr)) {
            info.setAutoBucket(true);
        } else {
            info.setBucketCount(Integer.parseInt(bucketStr));
        }
        
        return info;
    }
    
    /**
     * 解析属性设置
     */
    private List<StarRocksCreateTable.Property> parseProperties(String sql) {
        List<StarRocksCreateTable.Property> properties = new ArrayList<>();
        
        Matcher m = PROPERTIES_PATTERN.matcher(sql);
        while (m.find()) {
            StarRocksCreateTable.Property prop = new StarRocksCreateTable.Property();
            prop.setKey(m.group(1));
            prop.setValue(m.group(2));
            properties.add(prop);
        }
        
        return properties;
    }
    
    /**
     * 重写 parse 方法，支持 StarRocks 特有的 INSERT 语法
     */
    @Override
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        // 检测 StarRocks 特有的 INSERT OVERWRITE
        if (upper.startsWith("INSERT")) {
            Matcher m = INSERT_OVERWRITE_PATTERN.matcher(sql);
            if (m.find()) {
                boolean isOverwrite = "OVERWRITE".equalsIgnoreCase(m.group(1));
                
                // 调用父类解析基础 INSERT
                SqlStatement stmt = super.parse(sql);
                
                // 扩展为 StarRocksInsertDetails
                if (stmt.getInsertDetails() != null) {
                    StarRocksInsertDetails srDetails = new StarRocksInsertDetails();
                    copyInsertDetails(stmt.getInsertDetails(), srDetails);
                    srDetails.setOverwrite(isOverwrite);
                    
                    // 解析分区指定
                    Matcher partitionMatcher = PARTITION_SPEC_PATTERN.matcher(sql);
                    if (partitionMatcher.find()) {
                        String[] parts = partitionMatcher.group(1).split("\\s*,\\s*");
                        srDetails.setTargetPartitions(Arrays.asList(parts));
                    }
                    
                    stmt.setInsertDetails(srDetails);
                }
                
                return stmt;
            }
        }
        
        // 其他语句使用父类解析
        return super.parse(sql);
    }
    
    /**
     * 复制 InsertDetails 属性
     */
    private void copyInsertDetails(InsertDetails source, StarRocksInsertDetails target) {
        target.setTargetTable(source.getTargetTable());
        target.setTargetColumns(source.getTargetColumns());
        target.setMode(source.getMode());
        target.setValueRows(source.getValueRows());
        target.setSelectQuery(source.getSelectQuery());
        target.setColumnMappings(source.getColumnMappings());
        target.setOnConflict(source.getOnConflict());
    }
}
