package com.example.sqlparser.model.clickhouse;

import com.example.sqlparser.model.InsertDetails;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * ClickHouse 特有的 INSERT 扩展
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClickHouseInsertDetails extends InsertDetails {
    
    // INSERT INTO ... VALUES, ...
    private boolean allowExperimentalJsonType;
    
    // 表函数 (如 numbers, file, url 等)
    private boolean tableFunction;
    private String tableFunctionName;
    private List<String> tableFunctionArgs;
    
    // FORMAT
    private String format;
    
    // SETTINGS
    private List<Setting> settings;
    
    @Data
    public static class Setting {
        private String key;
        private String value;
    }
}
