package com.cc.argodb.dashboard.controller;

import com.cc.argodb.dashboard.model.ApiResponse;
import com.cc.argodb.dashboard.model.AssetStats;
import com.cc.argodb.dashboard.model.ConnectionConfig;
import com.cc.argodb.dashboard.model.DatabaseTree;
import com.cc.argodb.dashboard.service.ConnectionService;
import com.cc.argodb.dashboard.service.MetadataService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final ConnectionService connectionService;
    private final MetadataService metadataService;

    public MetadataController(ConnectionService connectionService, MetadataService metadataService) {
        this.connectionService = connectionService;
        this.metadataService = metadataService;
    }

    @GetMapping("/{connId}/full-tree")
    public ApiResponse<DatabaseTree> fullTree(@PathVariable String connId) {
        ConnectionConfig config = connectionService.findById(connId);
        if (config == null) {
            return ApiResponse.fail("连接配置不存在");
        }
        try {
            DatabaseTree tree = metadataService.fetchFullTree(config);
            return ApiResponse.ok(tree);
        } catch (Exception e) {
            return ApiResponse.fail("元数据查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{connId}/stats")
    public ApiResponse<AssetStats> stats(@PathVariable String connId) {
        ConnectionConfig config = connectionService.findById(connId);
        if (config == null) {
            return ApiResponse.fail("连接配置不存在");
        }
        try {
            AssetStats stats = metadataService.computeStats(config);
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.fail("统计查询失败: " + e.getMessage());
        }
    }
}
