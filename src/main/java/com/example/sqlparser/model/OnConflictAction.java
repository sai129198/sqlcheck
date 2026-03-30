package com.example.sqlparser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 冲突处理动作
 */
@Data
public class OnConflictAction {
    private ConflictTarget conflictTarget;
    private ConflictAction action;
    private List<Assignment> updateAssignments = new ArrayList<>();
}
