package com.cc.argodb.dashboard.model;

import lombok.Data;

@Data
public class ConnectionConfig {
    private String id;
    private String name;
    private String url;
    private String driverClassName;
    private String username;
    private String password;
    private String driverJarFile;
    private long createdAt;
    private long updatedAt;
}
