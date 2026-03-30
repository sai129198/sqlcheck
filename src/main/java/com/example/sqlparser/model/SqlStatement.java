package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 语句根对象
 */
@Data
public class SqlStatement {
    private StatementType type;
    
    // 共享元素
    private List<CteDefinition> ctes = new ArrayList<>();
    private List<SubQueryRef> subQueries = new ArrayList<>();
    
    // 各语句类型的详情（互斥）
    private SelectDetails selectDetails;
    private InsertDetails insertDetails;
    private UpdateDetails updateDetails;
    private DeleteDetails deleteDetails;
    private MergeDetails mergeDetails;

    public boolean isSelect() {
        return type == StatementType.SELECT;
    }

    public boolean isInsert() {
        return type == StatementType.INSERT;
    }

    public boolean isUpdate() {
        return type == StatementType.UPDATE;
    }

    public boolean isDelete() {
        return type == StatementType.DELETE;
    }

    public boolean isMerge() {
        return type == StatementType.MERGE;
    }

    /**
     * 获取查询涉及的所有表
     */
    public List<TableRef> getAllTables() {
        switch (type) {
            case SELECT:
                return selectDetails != null ? 
                    collectSelectTables(selectDetails.getQueryBlock()) : Collections.emptyList();
            case INSERT:
                return collectInsertTables();
            case UPDATE:
                return collectUpdateTables();
            case DELETE:
                return collectDeleteTables();
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取所有字段血缘
     */
    public List<ColumnLineage> getAllColumnLineage() {
        List<ColumnLineage> result = new ArrayList<>();
        
        switch (type) {
            case SELECT:
                if (selectDetails != null) {
                    collectSelectLineage(selectDetails.getQueryBlock(), result);
                }
                break;
            case INSERT:
                if (insertDetails != null) {
                    for (ColumnMapping mapping : insertDetails.getColumnMappings()) {
                        result.add(new ColumnLineage(mapping.getTargetColumn(), mapping.getSourceLineage()));
                    }
                }
                break;
            case UPDATE:
                if (updateDetails != null) {
                    result.addAll(updateDetails.getAssignments().stream()
                        .map(a -> new ColumnLineage(a.getTargetColumn(), a.getValueLineage()))
                        .collect(Collectors.toList()));
                }
                break;
            default:
                break;
        }
        
        return result;
    }

    private List<TableRef> collectSelectTables(QueryBlock query) {
        if (query == null) return Collections.emptyList();
        
        List<TableRef> tables = new ArrayList<>();
        
        if (query.getType() == QueryBlockType.SIMPLE_SELECT) {
            if (query.getFrom() != null) {
                tables.addAll(query.getFrom().getTables());
            }
        } else {
            tables.addAll(collectSelectTables(query.getLeftQuery()));
            tables.addAll(collectSelectTables(query.getRightQuery()));
        }
        
        return tables;
    }

    private List<TableRef> collectInsertTables() {
        List<TableRef> tables = new ArrayList<>();
        if (insertDetails != null) {
            TableRef target = new TableRef();
            target.setType(TableType.PHYSICAL_TABLE);
            target.setName(insertDetails.getTargetTable().getTableName());
            target.setAlias(insertDetails.getTargetTable().getAlias());
            target.setSchema(insertDetails.getTargetTable().getSchema());
            tables.add(target);
            
            if (insertDetails.getSelectQuery() != null) {
                tables.addAll(collectSelectTables(insertDetails.getSelectQuery()));
            }
        }
        return tables;
    }

    private List<TableRef> collectUpdateTables() {
        List<TableRef> tables = new ArrayList<>();
        if (updateDetails != null) {
            TableRef target = new TableRef();
            target.setType(TableType.PHYSICAL_TABLE);
            target.setName(updateDetails.getTargetTable().getTableName());
            target.setAlias(updateDetails.getTargetTable().getAlias());
            target.setSchema(updateDetails.getTargetTable().getSchema());
            tables.add(target);
            
            if (updateDetails.getFromClause() != null) {
                tables.addAll(updateDetails.getFromClause().getTables());
            }
        }
        return tables;
    }

    private List<TableRef> collectDeleteTables() {
        List<TableRef> tables = new ArrayList<>();
        if (deleteDetails != null) {
            TableRef target = new TableRef();
            target.setType(TableType.PHYSICAL_TABLE);
            target.setName(deleteDetails.getTargetTable().getTableName());
            target.setAlias(deleteDetails.getTargetTable().getAlias());
            target.setSchema(deleteDetails.getTargetTable().getSchema());
            tables.add(target);
            
            if (deleteDetails.getUsingClause() != null) {
                tables.addAll(deleteDetails.getUsingClause().getTables());
            }
        }
        return tables;
    }

    private void collectSelectLineage(QueryBlock query, List<ColumnLineage> result) {
        if (query == null) return;
        
        if (query.getType() == QueryBlockType.SIMPLE_SELECT) {
            if (query.getSelect() != null && query.getSelect().getColumns() != null) {
                for (ColumnRef col : query.getSelect().getColumns()) {
                    result.add(new ColumnLineage(col.getName(), col.getLineage()));
                }
            }
        } else {
            collectSelectLineage(query.getLeftQuery(), result);
            collectSelectLineage(query.getRightQuery(), result);
        }
    }
}
