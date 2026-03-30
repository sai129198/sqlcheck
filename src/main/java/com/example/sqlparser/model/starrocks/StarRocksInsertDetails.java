package com.example.sqlparser.model.starrocks;

import com.example.sqlparser.model.InsertDetails;

import java.util.List;

/**
 * StarRocks 特有的 INSERT 扩展
 */
public class StarRocksInsertDetails extends InsertDetails {
    
    // Stream Load 模式
    private boolean streamLoad;
    
    // Broker Load / Spark Load
    private boolean brokerLoad;
    private String loadLabel;
    private List<String> dataFiles;
    private String format;
    
    // Routine Load
    private boolean routineLoad;
    private String kafkaTopic;
    private String kafkaBrokers;
    
    // INSERT OVERWRITE
    private boolean overwrite;
    
    // 分区指定
    private List<String> targetPartitions;

    public boolean isStreamLoad() {
        return streamLoad;
    }

    public void setStreamLoad(boolean streamLoad) {
        this.streamLoad = streamLoad;
    }

    public boolean isBrokerLoad() {
        return brokerLoad;
    }

    public void setBrokerLoad(boolean brokerLoad) {
        this.brokerLoad = brokerLoad;
    }

    public String getLoadLabel() {
        return loadLabel;
    }

    public void setLoadLabel(String loadLabel) {
        this.loadLabel = loadLabel;
    }

    public List<String> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(List<String> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isRoutineLoad() {
        return routineLoad;
    }

    public void setRoutineLoad(boolean routineLoad) {
        this.routineLoad = routineLoad;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
    }

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public void setKafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public List<String> getTargetPartitions() {
        return targetPartitions;
    }

    public void setTargetPartitions(List<String> targetPartitions) {
        this.targetPartitions = targetPartitions;
    }
}
