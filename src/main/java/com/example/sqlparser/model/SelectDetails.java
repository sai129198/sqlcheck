package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SELECT 语句详情
 */
@Data
public class SelectDetails {
    private QueryBlock queryBlock;
    private QueryStructureType structureType;
    private SetOperationInfo setOperation;
}
