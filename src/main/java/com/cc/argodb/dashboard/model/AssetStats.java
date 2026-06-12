package com.cc.argodb.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetStats {

    private String connectionName;

    // === 资产概览 ===
    private int databaseCount;
    private int schemaCount;
    private int tableCount;
    private int viewCount;
    private int columnCount;
    private int partitionCount;
    private int bucketTableCount;
    private int materializedViewCount;
    private int udfCount;

    // === 模型健康度 ===
    private int tablesWithComment;
    private double tableCommentRate;
    private int columnsWithComment;
    private double columnCommentRate;
    private int emptyTables;
    private int wideTables50;
    private int wideTables100;
    private int tablesWithoutPrimaryKey;
    private int tablesWithOwner;          // 有归属人的表
    private double tableOwnerRate;
    private double modelHealthScore;

    // === 数据质量指标 ===
    private int qualityRuleCount;
    private double qualityCoverageRate;
    private double qualityPassRate7d;
    private int highRiskTables;

    // === 数据使用热度 ===
    private int hotTables30d;
    private int coldTables;
    private double coldRate;
    // 表类型分布
    private int transactionalTables;
    private int holodeskTables;
    private int textTables;

    // === 安全合规 ===
    private int sensitiveTables;
    private int unMaskedSensitiveColumns;
    private int overPermissionAccounts;
    private int backupCoveredTables;
    private double backupCoverageRate;
    // 行级/列级权限
    private int tablesWithRowPermission;
    private int tablesWithColumnPermission;

    // === 用户与权限 ===
    private int totalUsers;
    private int totalRoles;
    private int activeSessions;
    private int totalPrivileges;
    private List<UserInfo> topUsers = new ArrayList<>();
    private List<PrivilegeSummary> privilegeSummary = new ArrayList<>();

    // === 数据库状态 ===
    private String dbVersion;
    private String tdhVersion;
    private String dbProductName;
    private int totalPartitions;
    private List<DbInfo> databaseDetails = new ArrayList<>();

    // === 成本治理 ===
    private long totalEstimatedRows;
    private int governanceItemCount;

    // === Top N 列表 ===
    private List<TopTableInfo> topWideTables = new ArrayList<>();
    private List<TopTableInfo> topEmptyTables = new ArrayList<>();
    private List<String> governanceSuggestions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopTableInfo {
        private String database;
        private String tableName;
        private String tableType;
        private int columnCount;
        private long estimatedRows;
        private boolean hasComment;
        private String ownerName;
        private String lastLoadTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String userName;
        private int privilegeCount;
        private String lastActive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivilegeSummary {
        private String privilegeType;
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbInfo {
        private String dbName;
        private String ownerName;
        private int tableCount;
        private int viewCount;
    }
}
