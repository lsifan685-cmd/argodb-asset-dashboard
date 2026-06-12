package com.cc.argodb.dashboard.controller;

import com.cc.argodb.dashboard.model.ApiResponse;
import com.cc.argodb.dashboard.model.ConnectionConfig;
import com.cc.argodb.dashboard.service.ConnectionService;
import com.cc.argodb.dashboard.service.DriverService;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.List;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final DriverService driverService;

    public ConnectionController(ConnectionService connectionService, DriverService driverService) {
        this.connectionService = connectionService;
        this.driverService = driverService;
    }

    @GetMapping
    public ApiResponse<List<ConnectionConfig>> list() {
        return ApiResponse.ok(connectionService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ConnectionConfig> get(@PathVariable String id) {
        ConnectionConfig config = connectionService.findById(id);
        if (config == null) {
            return ApiResponse.fail("连接配置不存在");
        }
        return ApiResponse.ok(config);
    }

    @PostMapping
    public ApiResponse<ConnectionConfig> create(@RequestBody ConnectionConfig config) {
        return ApiResponse.ok(connectionService.save(config));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConnectionConfig> update(@PathVariable String id, @RequestBody ConnectionConfig config) {
        config.setId(id);
        return ApiResponse.ok(connectionService.save(config));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        connectionService.delete(id);
        driverService.evictCache(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/test")
    public ApiResponse<String> test(@RequestBody ConnectionConfig config) {
        try {
            // 测试连接时用临时 ID，不需要已保存
            String tempId = config.getId() != null ? config.getId() : "test_" + System.currentTimeMillis();
            Connection conn = driverService.createConnection(
                    tempId, config.getDriverClassName(), config.getUrl(),
                    config.getUsername(), config.getPassword(), config.getDriverJarFile());
            conn.close();
            driverService.evictCache(tempId);
            return ApiResponse.ok("连接成功");
        } catch (Exception e) {
            return ApiResponse.fail("连接失败: " + e.getMessage());
        }
    }
}
