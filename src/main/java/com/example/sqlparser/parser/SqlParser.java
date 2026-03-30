package com.example.sqlparser.parser;

import com.example.sqlparser.model.*;

import java.util.*;
import java.util.regex.*;

/**
 * SQL 解析器（简化版，基于正则和关键字解析）
 * 用于演示核心功能，生产环境建议使用 ANTLR4
 */
public class SqlParser {
    
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "SELECT\\s+(.*?)\\s+FROM\\s+(.*?)(?:\\s+WHERE\\s+(.*?))?(?:\\s+GROUP\\s+BY\\s+([^\\s]+(?:\\s*,\\s*[^\\s]+)*))?(?:\\s+ORDER\\s+BY\\s+(.+?))?(?:\\s+LIMIT\\s+(\\d+))?$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COLUMN_PATTERN = Pattern.compile(
        "(?:(\\w+)\\s*\\.\\s*)?(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析 SQL 语句
     */
    public SqlStatement parse(String sql) {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase();
        
        SqlStatement statement = new SqlStatement();
        
        // 检测 CTE
        if (upper.startsWith("WITH")) {
            int selectIndex = findSelectIndex(upper);
            if (selectIndex > 0) {
                String ctePart = trimmed.substring(4, selectIndex).trim();
                statement.setCtes(parseCtes(ctePart));
                trimmed = trimmed.substring(selectIndex);
                upper = trimmed.toUpperCase();
            }
        }
        
        // 检测语句类型
        if (upper.startsWith("SELECT")) {
            statement.setType(StatementType.SELECT);
            statement.setSelectDetails(parseSelect(trimmed));
        } else if (upper.startsWith("INSERT")) {
            statement.setType(StatementType.INSERT);
            statement.setInsertDetails(parseInsert(trimmed));
        } else if (upper.startsWith("UPDATE")) {
            statement.setType(StatementType.UPDATE);
            statement.setUpdateDetails(parseUpdate(trimmed));
        } else if (upper.startsWith("DELETE")) {
            statement.setType(StatementType.DELETE);
            statement.setDeleteDetails(parseDelete(trimmed));
        } else {
            throw new IllegalArgumentException("Unsupported SQL statement: " + sql.substring(0, Math.min(50, sql.length())));
        }
        
        return statement;
    }
    
    private int findSelectIndex(String upper) {
        // 找到主 SELECT 的位置（跳过 CTE 中的子查询）
        int depth = 0;
        for (int i = 0; i < upper.length() - 6; i++) {
            char c = upper.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (depth == 0 && upper.substring(i).startsWith("SELECT")) {
                return i;
            }
        }
        return -1;
    }
    
    private List<CteDefinition> parseCtes(String ctePart) {
        List<CteDefinition> ctes = new ArrayList<>();
        // 简化实现：假设 CTE 格式为 name AS (SELECT ...)
        String[] parts = ctePart.split("(?i),\\s*(?=\\w+\\s+AS)");
        for (String part : parts) {
            CteDefinition cte = new CteDefinition();
            Matcher m = Pattern.compile("(\\w+)\\s+AS\\s*\\((.+)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(part.trim());
            if (m.find()) {
                cte.setName(m.group(1));
                // 递归解析 CTE 查询
                SelectDetails details = parseSelect(m.group(2));
                if (details != null) {
                    cte.setQuery(details.getQueryBlock());
                }
                ctes.add(cte);
            }
        }
        return ctes;
    }
    
    private SelectDetails parseSelect(String sql) {
        SelectDetails details = new SelectDetails();
        
        // 检测 UNION
        String upper = sql.toUpperCase();
        if (upper.contains(" UNION ")) {
            return parseUnion(sql);
        }
        
        details.setStructureType(QueryStructureType.SIMPLE);
        
        Matcher m = SELECT_PATTERN.matcher(sql);
        if (!m.find()) {
            // 尝试简化匹配
            return parseSimpleSelect(sql);
        }
        
        QueryBlock query = new QueryBlock();
        query.setType(QueryBlockType.SIMPLE_SELECT);
        
        // SELECT 子句
        String selectPart = m.group(1).trim();
        query.setSelect(parseSelectClause(selectPart));
        
        // FROM 子句
        String fromPart = m.group(2).trim();
        query.setFrom(parseFromClause(fromPart));
        
        // WHERE 子句
        if (m.group(3) != null) {
            WhereClause where = new WhereClause();
            where.setConditionLineage(parseExpression(m.group(3).trim()));
            query.setWhere(where);
        }
        
        // GROUP BY 子句
        if (m.group(4) != null) {
            query.setGroupBy(parseGroupBy(m.group(4).trim()));
        }
        
        // ORDER BY 子句
        if (m.group(5) != null) {
            query.setOrderBy(parseOrderBy(m.group(5).trim()));
        }
        
        // LIMIT 子句
        if (m.group(6) != null) {
            LimitClause limit = new LimitClause();
            limit.setLimit(Long.parseLong(m.group(6)));
            query.setLimit(limit);
        }
        
        details.setQueryBlock(query);
        return details;
    }
    
    private SelectDetails parseUnion(String sql) {
        SelectDetails details = new SelectDetails();
        details.setStructureType(QueryStructureType.SET_OPERATION);
        
        String[] parts = sql.split("(?i)\\s+UNION\\s+");
        
        QueryBlock query = new QueryBlock();
        query.setType(QueryBlockType.UNION);
        query.setSetOpType(SetOperationType.UNION);
        query.setSetOpAll(sql.toUpperCase().contains("UNION ALL"));
        
        if (parts.length >= 2) {
            SelectDetails left = parseSelect(parts[0].trim());
            SelectDetails right = parseSelect(parts[1].trim());
            query.setLeftQuery(left.getQueryBlock());
            query.setRightQuery(right.getQueryBlock());
        }
        
        details.setQueryBlock(query);
        return details;
    }
    
    private SelectDetails parseSimpleSelect(String sql) {
        // 最简化的 SELECT 解析
        SelectDetails details = new SelectDetails();
        details.setStructureType(QueryStructureType.SIMPLE);
        
        QueryBlock query = new QueryBlock();
        query.setType(QueryBlockType.SIMPLE_SELECT);
        
        String upper = sql.toUpperCase();
        
        // 提取 SELECT 和 FROM 之间的内容
        int selectIdx = upper.indexOf("SELECT") + 6;
        int fromIdx = upper.indexOf("FROM");
        
        if (fromIdx > selectIdx) {
            String selectPart = sql.substring(selectIdx, fromIdx).trim();
            query.setSelect(parseSelectClause(selectPart));
            
            String rest = sql.substring(fromIdx + 4).trim();
            
            // 检测各子句的位置
            int whereIdx = rest.toUpperCase().indexOf("WHERE");
            int groupByIdx = rest.toUpperCase().indexOf("GROUP BY");
            int orderByIdx = rest.toUpperCase().indexOf("ORDER BY");
            int limitIdx = rest.toUpperCase().indexOf("LIMIT");
            
            // 确定 FROM 子句的结束位置
            int fromEnd = rest.length();
            if (whereIdx >= 0) fromEnd = Math.min(fromEnd, whereIdx);
            if (groupByIdx >= 0) fromEnd = Math.min(fromEnd, groupByIdx);
            if (orderByIdx >= 0) fromEnd = Math.min(fromEnd, orderByIdx);
            if (limitIdx >= 0) fromEnd = Math.min(fromEnd, limitIdx);
            
            String fromPart = rest.substring(0, fromEnd).trim();
            query.setFrom(parseFromClause(fromPart));
            
            // 解析 WHERE
            if (whereIdx >= 0) {
                int whereEnd = rest.length();
                if (groupByIdx >= 0) whereEnd = Math.min(whereEnd, groupByIdx);
                if (orderByIdx >= 0) whereEnd = Math.min(whereEnd, orderByIdx);
                if (limitIdx >= 0) whereEnd = Math.min(whereEnd, limitIdx);
                
                String wherePart = rest.substring(whereIdx + 5, whereEnd).trim();
                WhereClause where = new WhereClause();
                where.setConditionLineage(parseExpression(wherePart));
                query.setWhere(where);
            }
            
            // 解析 ORDER BY
            if (orderByIdx >= 0) {
                int orderEnd = rest.length();
                if (limitIdx >= 0) orderEnd = Math.min(orderEnd, limitIdx);
                
                String orderPart = rest.substring(orderByIdx + 8, orderEnd).trim();
                query.setOrderBy(parseOrderBy(orderPart));
            }
            
            // 解析 LIMIT
            if (limitIdx >= 0) {
                String limitPart = rest.substring(limitIdx + 5).trim();
                LimitClause limit = new LimitClause();
                limit.setLimit(Long.parseLong(limitPart.split("\\s+")[0]));
                query.setLimit(limit);
            }
        }
        
        details.setQueryBlock(query);
        return details;
    }
    
    private SelectClause parseSelectClause(String selectPart) {
        SelectClause clause = new SelectClause();
        clause.setDistinct(selectPart.toUpperCase().contains("DISTINCT"));
        
        // 移除 DISTINCT 关键字
        String cols = selectPart.replaceAll("(?i)DISTINCT\\s+", "").trim();
        
        // 分割字段（简化处理，不考虑函数内的逗号）
        String[] columns = cols.split("\\s*,\\s*");
        for (String col : columns) {
            clause.getColumns().add(parseColumn(col.trim()));
        }
        
        return clause;
    }
    
    private ColumnRef parseColumn(String colStr) {
        ColumnRef col = new ColumnRef();
        
        // 检测 AS 别名
        String[] parts = colStr.split("(?i)\\s+AS\\s+");
        String expr = parts[0].trim();
        if (parts.length > 1) {
            col.setAlias(parts[1].trim());
        }
        
        // 检测简单表达式
        col.setLineage(parseExpression(expr));
        
        // 提取名称
        Matcher m = COLUMN_PATTERN.matcher(expr);
        if (m.find()) {
            col.setName(col.getAlias() != null ? col.getAlias() : m.group(2));
            if (m.group(3) != null && col.getAlias() == null) {
                col.setAlias(m.group(3));
            }
        } else {
            col.setName(expr);
        }
        
        return col;
    }
    
    private FromClause parseFromClause(String fromPart) {
        FromClause clause = new FromClause();
        
        // 简化处理：只处理单表和简单 JOIN
        String[] tables = fromPart.split("(?i)\\s*,\\s*");
        
        for (String table : tables) {
            table = table.trim();
            
            // 检测 JOIN
            if (table.toUpperCase().contains(" JOIN ")) {
                String[] joinParts = table.split("(?i)\\s+JOIN\\s+");
                if (joinParts.length >= 2) {
                    TableRef left = parseTableRef(joinParts[0].trim());
                    clause.getTables().add(left);
                    
                    // 解析 JOIN 右侧和条件
                    String rightPart = joinParts[1];
                    String onKeyword = "(?i)\\s+ON\\s+";
                    String[] rightAndOn = rightPart.split(onKeyword, 2);
                    
                    TableRef right = parseTableRef(rightAndOn[0].trim());
                    // 右表也添加到 tables 列表
                    clause.getTables().add(right);
                    
                    JoinCondition join = new JoinCondition();
                    join.setJoinType(JoinType.INNER);
                    join.setRightTable(right);
                    
                    if (rightAndOn.length > 1) {
                        join.setCondition(parseExpression(rightAndOn[1].trim()));
                    }
                    
                    clause.getJoins().add(join);
                }
            } else {
                clause.getTables().add(parseTableRef(table));
            }
        }
        
        return clause;
    }
    
    private TableRef parseTableRef(String tableStr) {
        TableRef table = new TableRef();
        table.setType(TableType.PHYSICAL_TABLE);
        
        Matcher m = TABLE_PATTERN.matcher(tableStr);
        if (m.find()) {
            table.setName(m.group(1));
            if (m.group(2) != null) {
                table.setAlias(m.group(2));
            }
        } else {
            table.setName(tableStr);
        }
        
        return table;
    }
    
    private GroupByClause parseGroupBy(String groupByPart) {
        GroupByClause clause = new GroupByClause();
        
        String[] cols = groupByPart.split("\\s*,\\s*");
        for (String col : cols) {
            ColumnRef ref = new ColumnRef();
            ref.setName(col.trim());
            clause.getColumns().add(ref);
        }
        
        return clause;
    }
    
    private OrderByClause parseOrderBy(String orderByPart) {
        OrderByClause clause = new OrderByClause();
        
        String[] items = orderByPart.split("\\s*,\\s*");
        for (String item : items) {
            OrderByItem obi = new OrderByItem();
            
            String upper = item.toUpperCase();
            if (upper.endsWith(" DESC")) {
                obi.setAscending(false);
                item = item.substring(0, item.length() - 5).trim();
            } else if (upper.endsWith(" ASC")) {
                item = item.substring(0, item.length() - 4).trim();
            }
            
            ColumnRef col = new ColumnRef();
            col.setName(item);
            obi.setColumn(col);
            
            clause.getItems().add(obi);
        }
        
        return clause;
    }
    
    private LineageInfo parseExpression(String expr) {
        LineageInfo info = new LineageInfo();
        info.setOriginalExpression(expr);
        
        // 简单表达式解析
        List<LineageSource> sources = new ArrayList<>();
        
        // 提取字段引用（格式：table.column 或 column）
        Matcher m = Pattern.compile("(\\w+)\\s*\\.\\s*(\\w+)").matcher(expr);
        while (m.find()) {
            LineageSource source = new LineageSource();
            source.setType(SourceType.TABLE_COLUMN);
            
            TableColumnRef tableCol = new TableColumnRef();
            tableCol.setTableName(m.group(1));
            tableCol.setColumnName(m.group(2));
            source.setTableColumn(tableCol);
            
            sources.add(source);
        }
        
        // 如果没有 table.column 格式，尝试提取简单字段名
        if (sources.isEmpty()) {
            m = Pattern.compile("\\b(\\w+)\\b").matcher(expr);
            while (m.find()) {
                String word = m.group(1);
                // 跳过 SQL 关键字
                if (isSqlKeyword(word)) continue;
                
                LineageSource source = new LineageSource();
                source.setType(SourceType.TABLE_COLUMN);
                
                TableColumnRef tableCol = new TableColumnRef();
                tableCol.setColumnName(word);
                source.setTableColumn(tableCol);
                
                sources.add(source);
            }
        }
        
        info.setSources(sources);
        
        // 判断表达式类型
        if (expr.contains("+")) info.setExpressionType(ExpressionType.ADD);
        else if (expr.contains("-")) info.setExpressionType(ExpressionType.SUBTRACT);
        else if (expr.contains("*")) info.setExpressionType(ExpressionType.MULTIPLY);
        else if (expr.contains("/")) info.setExpressionType(ExpressionType.DIVIDE);
        else if (expr.contains("=")) info.setExpressionType(ExpressionType.EQUAL);
        else info.setExpressionType(ExpressionType.COLUMN);
        
        return info;
    }
    
    private boolean isSqlKeyword(String word) {
        Set<String> keywords = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "NULL",
            "INSERT", "UPDATE", "DELETE", "SET", "VALUES",
            "JOIN", "ON", "AS", "DISTINCT", "ALL"
        ));
        return keywords.contains(word.toUpperCase());
    }
    
    private InsertDetails parseInsert(String sql) {
        InsertDetails details = new InsertDetails();
        
        String upper = sql.toUpperCase();
        
        // 解析目标表
        Pattern p = Pattern.compile("INSERT\\s+INTO\\s+(\\w+)(?:\\s*\\(([^)]+)\\))?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        
        if (m.find()) {
            TargetTable target = new TargetTable();
            target.setTableName(m.group(1));
            details.setTargetTable(target);
            
            if (m.group(2) != null) {
                String[] cols = m.group(2).split("\\s*,\\s*");
                for (String col : cols) {
                    details.getTargetColumns().add(col.trim());
                }
            }
        }
        
        // 检测 VALUES 或 SELECT
        if (upper.contains("VALUES")) {
            details.setMode(InsertMode.VALUES);
            parseInsertValues(sql, details);
        } else if (upper.contains("SELECT")) {
            details.setMode(InsertMode.SELECT);
            int selectIdx = upper.indexOf("SELECT");
            String selectSql = sql.substring(selectIdx);
            SelectDetails selectDetails = parseSelect(selectSql);
            details.setSelectQuery(selectDetails.getQueryBlock());
            
            // 构建字段映射
            buildInsertColumnMappings(details);
        }
        
        return details;
    }
    
    private void parseInsertValues(String sql, InsertDetails details) {
        Pattern p = Pattern.compile("VALUES\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        
        while (m.find()) {
            String[] values = m.group(1).split("\\s*,\\s*");
            List<ValueExpression> row = new ArrayList<>();
            
            for (String val : values) {
                ValueExpression ve = new ValueExpression();
                val = val.trim();
                
                if (val.equalsIgnoreCase("NULL")) {
                    ve.setType(ValueType.NULL);
                } else if (val.equalsIgnoreCase("DEFAULT")) {
                    ve.setType(ValueType.DEFAULT);
                } else if (val.startsWith("'") || val.startsWith("\"") || 
                          val.matches("-?\\d+(\\.\\d+)?")) {
                    ve.setType(ValueType.CONSTANT);
                    ve.setValue(val);
                } else {
                    ve.setType(ValueType.EXPRESSION);
                    ve.setExpression(parseExpression(val));
                }
                
                row.add(ve);
            }
            
            details.getValueRows().add(row);
        }
    }
    
    private void buildInsertColumnMappings(InsertDetails details) {
        if (details.getSelectQuery() == null) return;
        
        List<ColumnRef> sourceCols = details.getSelectQuery().getSelect().getColumns();
        List<String> targetCols = details.getTargetColumns();
        
        for (int i = 0; i < targetCols.size() && i < sourceCols.size(); i++) {
            ColumnMapping mapping = new ColumnMapping();
            mapping.setTargetColumn(targetCols.get(i));
            mapping.setSourceLineage(sourceCols.get(i).getLineage());
            details.getColumnMappings().add(mapping);
        }
    }
    
    private UpdateDetails parseUpdate(String sql) {
        UpdateDetails details = new UpdateDetails();
        
        // 解析目标表
        Pattern p = Pattern.compile("UPDATE\\s+(\\w+)(?:\\s+(?:AS\\s+)?(\\w+))?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        
        if (m.find()) {
            TargetTable target = new TargetTable();
            target.setTableName(m.group(1));
            if (m.group(2) != null) {
                target.setAlias(m.group(2));
            }
            details.setTargetTable(target);
        }
        
        // 解析 SET
        int setIdx = sql.toUpperCase().indexOf("SET");
        int whereIdx = sql.toUpperCase().indexOf("WHERE");
        
        String setPart;
        if (whereIdx > setIdx) {
            setPart = sql.substring(setIdx + 3, whereIdx).trim();
        } else {
            setPart = sql.substring(setIdx + 3).trim();
        }
        
        String[] assignments = setPart.split("\\s*,\\s*");
        for (String assign : assignments) {
            String[] parts = assign.split("\\s*=\\s*", 2);
            if (parts.length == 2) {
                Assignment a = new Assignment();
                a.setTargetColumn(parts[0].trim());
                a.setValueLineage(parseExpression(parts[1].trim()));
                a.setSourceType(AssignmentSourceType.EXPRESSION);
                details.getAssignments().add(a);
            }
        }
        
        // 解析 WHERE
        if (whereIdx >= 0) {
            WhereClause where = new WhereClause();
            where.setConditionLineage(parseExpression(sql.substring(whereIdx + 5).trim()));
            details.setWhere(where);
        }
        
        return details;
    }
    
    private DeleteDetails parseDelete(String sql) {
        DeleteDetails details = new DeleteDetails();
        
        String upper = sql.toUpperCase();
        
        // 解析目标表（支持 DELETE FROM table 或 DELETE table）
        Pattern p = Pattern.compile("DELETE\\s+(?:FROM\\s+)?(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        
        if (m.find()) {
            TargetTable target = new TargetTable();
            target.setTableName(m.group(1));
            details.setTargetTable(target);
        }
        
        // 解析 WHERE
        int whereIdx = upper.indexOf("WHERE");
        if (whereIdx >= 0) {
            WhereClause where = new WhereClause();
            where.setConditionLineage(parseExpression(sql.substring(whereIdx + 5).trim()));
            details.setWhere(where);
        }
        
        return details;
    }
}
