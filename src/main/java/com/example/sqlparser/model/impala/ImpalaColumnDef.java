package com.example.sqlparser.model.impala;

/**
 * Impala 列定义
 */
public class ImpalaColumnDef {
    private String name;
    private String dataType;
    private String comment;
    private boolean nullable = true;
    private String defaultValue;
    private String encoding;
    private Integer compressionCodec;
    private Integer blockSize;
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public Integer getCompressionCodec() {
        return compressionCodec;
    }
    
    public void setCompressionCodec(Integer compressionCodec) {
        this.compressionCodec = compressionCodec;
    }
    
    public Integer getBlockSize() {
        return blockSize;
    }
    
    public void setBlockSize(Integer blockSize) {
        this.blockSize = blockSize;
    }
    
    @Override
    public String toString() {
        return "ImpalaColumnDef{" +
                "name='" + name + '\'' +
                ", dataType='" + dataType + '\'' +
                ", comment='" + comment + '\'' +
                ", nullable=" + nullable +
                '}';
    }
}
