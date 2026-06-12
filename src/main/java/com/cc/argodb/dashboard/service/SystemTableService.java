package com.cc.argodb.dashboard.service;

import com.cc.argodb.dashboard.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class SystemTableService {

    private static final Logger log = LoggerFactory.getLogger(SystemTableService.class);

    private final DriverService driverService;

    public SystemTableService(DriverService driverService) {
        this.driverService = driverService;
    }

    public void enrichStats(AssetStats s, ConnectionConfig config) {
        try {
            Connection conn = driverService.createConnection(
                    config.getId(), config.getDriverClassName(), config.getUrl(),
                    config.getUsername(), config.getPassword(), config.getDriverJarFile());
            try {
                enrichStats(s, conn);
            } finally {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        } catch (Exception e) {
            log.warn("从 system 表获取扩展统计失败: {}", e.getMessage());
        }
    }

    /** 复用已有连接，避免重复建连开销 */
    public void enrichStats(AssetStats s, Connection conn) {
        try {
            enrichVersion(s, conn);
            enrichDatabases(s, conn);
            enrichTablesExtended(s, conn);
            enrichProcessesAndUsers(s, conn);
            enrichRoles(s, conn);
            enrichPartitionsAndBuckets(s, conn);
            enrichFunctions(s, conn);
            enrichMaterializedViews(s, conn);
        } catch (Exception e) {
            log.warn("从 system 表获取扩展统计失败: {}", e.getMessage());
        }
    }

    private void enrichVersion(AssetStats s, Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM system.version_v")) {
            if (rs.next()) {
                s.setDbVersion(rs.getString("schema_version"));
                s.setTdhVersion(rs.getString("tdh_version"));
            }
        } catch (SQLException e) {
            log.debug("获取版本信息失败: {}", e.getMessage());
        }
    }

    private void enrichDatabases(AssetStats s, Connection conn) throws SQLException {
        List<AssetStats.DbInfo> dbInfos = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT database_name, owner_name, owner_type FROM system.databases_v")) {
            while (rs.next()) {
                String dbName = rs.getString("database_name");
                if (isSystemDb(dbName)) continue;
                dbInfos.add(new AssetStats.DbInfo(dbName, rs.getString("owner_name"), 0, 0));
            }
        } catch (SQLException e) {
            log.debug("获取数据库详情失败: {}", e.getMessage());
        }

        // 一次性统计每库的表数和视图数 (替代 N 次逐库查询)
        java.util.Map<String, int[]> dbCounts = new java.util.LinkedHashMap<>();
        for (AssetStats.DbInfo di : dbInfos) {
            dbCounts.put(di.getDbName(), new int[]{0, 0});
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT database_name, table_type, COUNT(*) cnt FROM system.tables_v GROUP BY database_name, table_type")) {
            while (rs.next()) {
                String dbName = rs.getString("database_name");
                int[] counts = dbCounts.get(dbName);
                if (counts == null) continue;
                String type = rs.getString("table_type");
                int cnt = rs.getInt("cnt");
                if ("VIEW".equalsIgnoreCase(type) || "VIRTUAL_VIEW".equalsIgnoreCase(type)) {
                    counts[1] += cnt;
                } else {
                    counts[0] += cnt;
                }
            }
        } catch (SQLException e) {
            log.debug("批量统计表数失败: {}", e.getMessage());
        }
        for (AssetStats.DbInfo dbInfo : dbInfos) {
            int[] counts = dbCounts.get(dbInfo.getDbName());
            if (counts != null) {
                dbInfo.setTableCount(counts[0]);
                dbInfo.setViewCount(counts[1]);
            }
        }
        s.setDatabaseDetails(dbInfos);
    }

    private void enrichTablesExtended(AssetStats s, Connection conn) throws SQLException {
        int withOwner = 0, transactional = 0, holodesk = 0, text = 0;
        int rowPerm = 0, colPerm = 0;
        Map<String, AssetStats.TopTableInfo> topInfoMap = new LinkedHashMap<>();

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT table_name, database_name, table_type, owner_name, commentstring, transactional, table_format, last_load_time, row_permission, column_permission FROM system.tables_v")) {
            while (rs.next()) {
                String db = rs.getString("database_name");
                if (isSystemDb(db)) continue;
                String tName = rs.getString("table_name");
                String owner = rs.getString("owner_name");
                String comment = rs.getString("commentstring");
                String format = rs.getString("table_format");
                String trans = rs.getString("transactional");
                String rowP = rs.getString("row_permission");
                String colP = rs.getString("column_permission");
                String llTime = rs.getString("last_load_time");
                String tType = rs.getString("table_type");

                if (owner != null && !owner.isEmpty() && !"PUBLIC".equalsIgnoreCase(owner)) withOwner++;
                if ("true".equalsIgnoreCase(trans)) transactional++;
                if (format != null && format.toLowerCase().contains("holodesk")) holodesk++;
                else if (format != null && format.toLowerCase().contains("text")) text++;
                if (rowP != null && !rowP.isEmpty()) rowPerm++;
                if (colP != null && !colP.isEmpty()) colPerm++;

                if (!"VIEW".equalsIgnoreCase(tType) && !"VIRTUAL_VIEW".equalsIgnoreCase(tType)) {
                    String key = db + "." + tName;
                    topInfoMap.put(key, new AssetStats.TopTableInfo(db, tName, tType, 0, 0,
                            comment != null && !comment.isEmpty(), owner, llTime));
                }
            }
        } catch (SQLException e) {
            log.debug("获取扩展表信息失败: {}", e.getMessage());
        }

        s.setTablesWithOwner(withOwner);
        s.setTableOwnerRate(s.getTableCount() > 0 ? (double) withOwner / s.getTableCount() : 0);
        s.setTransactionalTables(transactional);
        s.setHolodeskTables(holodesk);
        s.setTextTables(text);
        s.setTablesWithRowPermission(rowPerm);
        s.setTablesWithColumnPermission(colPerm);

        // 补充 owner 到 Top 宽表
        for (AssetStats.TopTableInfo wide : s.getTopWideTables()) {
            AssetStats.TopTableInfo info = topInfoMap.get(wide.getDatabase() + "." + wide.getTableName());
            if (info != null) {
                wide.setOwnerName(info.getOwnerName());
                wide.setLastLoadTime(info.getLastLoadTime());
            }
        }
    }

    private void enrichProcessesAndUsers(AssetStats s, Connection conn) throws SQLException {
        Set<String> users = new LinkedHashSet<>();
        Map<String, String> userLastActive = new LinkedHashMap<>();
        int sessions = 0;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT user_name, last_access_time FROM system.processes_v")) {
            while (rs.next()) {
                String user = rs.getString("user_name");
                if (user != null) {
                    users.add(user);
                    userLastActive.putIfAbsent(user, rs.getString("last_access_time"));
                    sessions++;
                }
            }
        } catch (SQLException e) {
            log.debug("获取进程信息失败: {}", e.getMessage());
        }

        s.setActiveSessions(sessions);

        // 一次扫描 table_privileges_v 同时完成: 用户列表、每人权限数、权限类型分布、总权限数
        Map<String, Integer> userPrivCnts = new LinkedHashMap<>();
        Map<String, Integer> privTypeCnts = new LinkedHashMap<>();
        int totalPrivs = 0;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT principal_name, principal_type, table_privilege FROM system.table_privileges_v")) {
            while (rs.next()) {
                String name = rs.getString("principal_name");
                String type = rs.getString("principal_type");
                String priv = rs.getString("table_privilege");
                if ("USER".equalsIgnoreCase(type) && name != null) {
                    users.add(name);
                    userPrivCnts.merge(name, 1, Integer::sum);
                }
                if (priv != null) {
                    privTypeCnts.merge(priv, 1, Integer::sum);
                    totalPrivs++;
                }
            }
        } catch (SQLException e) {
            log.debug("批量获取权限信息失败: {}", e.getMessage());
        }

        s.setTotalUsers(users.size());
        s.setTotalPrivileges(totalPrivs);

        List<AssetStats.UserInfo> userInfos = new ArrayList<>();
        for (String user : users) {
            int privCnt = userPrivCnts.getOrDefault(user, 0);
            userInfos.add(new AssetStats.UserInfo(user, privCnt, userLastActive.getOrDefault(user, "")));
        }
        userInfos.sort((a, b) -> Integer.compare(b.getPrivilegeCount(), a.getPrivilegeCount()));
        s.setTopUsers(userInfos.size() > 10 ? userInfos.subList(0, 10) : userInfos);

        List<AssetStats.PrivilegeSummary> summary = new ArrayList<>();
        for (Map.Entry<String, Integer> e : privTypeCnts.entrySet()) {
            summary.add(new AssetStats.PrivilegeSummary(e.getKey(), e.getValue()));
        }
        s.setPrivilegeSummary(summary);
    }

    private void enrichRoles(AssetStats s, Connection conn) {
        int roles = 0;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SHOW ROLES")) {
            while (rs.next()) roles++;
        } catch (SQLException e) {
            log.debug("获取角色失败: {}", e.getMessage());
        }
        s.setTotalRoles(roles);
    }

    private void enrichPartitionsAndBuckets(AssetStats s, Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(DISTINCT table_name) AS table_cnt, COUNT(*) AS total_cnt FROM system.partitions_v")) {
            if (rs.next()) {
                s.setPartitionCount(rs.getInt("table_cnt"));
                s.setTotalPartitions(rs.getInt("total_cnt"));
            }
        } catch (SQLException e) {
            log.debug("获取分区信息失败: {}", e.getMessage());
        }

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT table_name) cnt FROM system.buckets_v")) {
            if (rs.next()) s.setBucketTableCount(rs.getInt("cnt"));
        } catch (SQLException e) {
            log.debug("获取分桶信息失败: {}", e.getMessage());
        }
    }

    private void enrichFunctions(AssetStats s, Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) cnt FROM system.functions_v")) {
            if (rs.next()) s.setUdfCount(rs.getInt("cnt"));
        } catch (SQLException e) {
            log.debug("获取UDF信息失败: {}", e.getMessage());
        }
    }

    private void enrichMaterializedViews(AssetStats s, Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) cnt FROM system.materialized_views_v")) {
            if (rs.next()) s.setMaterializedViewCount(rs.getInt("cnt"));
        } catch (SQLException e) {
            log.debug("获取物化视图信息失败: {}", e.getMessage());
        }
    }

    private boolean isSystemDb(String name) {
        return "system".equalsIgnoreCase(name) || "default".equalsIgnoreCase(name);
    }
}
