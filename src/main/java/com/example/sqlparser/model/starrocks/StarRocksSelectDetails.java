package com.example.sqlparser.model.starrocks;

import com.example.sqlparser.model.SelectDetails;

import java.util.List;

/**
 * StarRocks 特有的 SELECT 扩展
 */
public class StarRocksSelectDetails extends SelectDetails {
    
    // Query Hint
    private List<QueryHint> hints;
    
    // Pipeline 执行引擎相关
    private boolean enablePipeline;
    private int pipelineDop;
    
    // 查询队列相关
    private int queryQueueTimeout;
    
    // 资源组
    private String resourceGroup;

    public List<QueryHint> getHints() {
        return hints;
    }

    public void setHints(List<QueryHint> hints) {
        this.hints = hints;
    }

    public boolean isEnablePipeline() {
        return enablePipeline;
    }

    public void setEnablePipeline(boolean enablePipeline) {
        this.enablePipeline = enablePipeline;
    }

    public int getPipelineDop() {
        return pipelineDop;
    }

    public void setPipelineDop(int pipelineDop) {
        this.pipelineDop = pipelineDop;
    }

    public int getQueryQueueTimeout() {
        return queryQueueTimeout;
    }

    public void setQueryQueueTimeout(int queryQueueTimeout) {
        this.queryQueueTimeout = queryQueueTimeout;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public static class QueryHint {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
