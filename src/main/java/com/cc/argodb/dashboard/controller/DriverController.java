package com.cc.argodb.dashboard.controller;

import com.cc.argodb.dashboard.model.ApiResponse;
import com.cc.argodb.dashboard.service.DriverService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = driverService.saveDriver(file);
            return ApiResponse.ok(fileName);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (IOException e) {
            return ApiResponse.fail("驱动文件保存失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<String>> list() {
        try {
            return ApiResponse.ok(driverService.listDrivers());
        } catch (IOException e) {
            return ApiResponse.fail("获取驱动列表失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{fileName}")
    public ApiResponse<Void> delete(@PathVariable String fileName) {
        try {
            driverService.deleteDriver(fileName);
            return ApiResponse.ok(null);
        } catch (IOException e) {
            return ApiResponse.fail("删除驱动失败: " + e.getMessage());
        }
    }
}
