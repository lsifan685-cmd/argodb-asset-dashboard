package com.cc.argodb.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DatabaseMetadata {
    private String name;
    private List<SchemaMetadata> schemas;
}
