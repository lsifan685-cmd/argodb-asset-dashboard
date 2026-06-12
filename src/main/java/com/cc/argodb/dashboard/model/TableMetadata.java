package com.cc.argodb.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TableMetadata {
    private String name;
    private String comment;
    private String type;
    private List<ColumnMetadata> columns;
}
