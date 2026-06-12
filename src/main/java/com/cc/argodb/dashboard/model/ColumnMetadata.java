package com.cc.argodb.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ColumnMetadata {
    private String name;
    private String type;
    private int size;
    private boolean nullable;
    private String comment;
}
