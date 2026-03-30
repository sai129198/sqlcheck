package com.example.sqlparser.model.presto;

import com.example.sqlparser.model.TableRef;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Presto 表引用（扩展支持连接器、Schema等）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrestoTableRef extends TableRef {
    
    // Presto 特有的 catalog.schema.table 三层结构
    private String catalog;
    private String schema;
    
    // 连接器类型
    private String connector;
    
    // 是否为视图
    private boolean view;
    
    // 是否为物化视图
    private boolean materializedView;
    
    // 表属性
    private List<TableProperty> properties;
    
    @Data
    public static class TableProperty {
        private String key;
        private String value;
    }
}
