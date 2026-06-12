package com.cc.argodb.dashboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);
    private static final Path DRIVER_DIR = Paths.get(System.getProperty("user.dir"), "data", "drivers");

    private final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();

    public DriverService() {
        try {
            Files.createDirectories(DRIVER_DIR);
        } catch (IOException e) {
            throw new RuntimeException("无法创建驱动目录", e);
        }
    }

    public String saveDriver(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.endsWith(".jar")) {
            throw new IllegalArgumentException("只支持 .jar 文件");
        }
        String safeName = Paths.get(fileName).getFileName().toString();
        Path target = DRIVER_DIR.resolve(safeName).normalize();
        if (!target.startsWith(DRIVER_DIR.normalize())) {
            throw new IllegalArgumentException("非法的文件名");
        }
        file.transferTo(target.toFile());
        log.info("驱动已保存: {}", target);
        return safeName;
    }

    public List<String> listDrivers() throws IOException {
        if (!Files.exists(DRIVER_DIR)) {
            return new ArrayList<>();
        }
        return Files.list(DRIVER_DIR)
                .filter(p -> p.toString().endsWith(".jar"))
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    public void deleteDriver(String fileName) throws IOException {
        String safeName = Paths.get(fileName).getFileName().toString();
        Path path = DRIVER_DIR.resolve(safeName).normalize();
        if (!path.startsWith(DRIVER_DIR.normalize())) {
            throw new IllegalArgumentException("非法的文件名");
        }
        Files.deleteIfExists(path);
    }

    public Path getDriverPath(String fileName) {
        return DRIVER_DIR.resolve(fileName);
    }

    public Connection createConnection(String connId, String driverClassName, String url,
                                        String username, String password, String driverJarFile) throws Exception {
        Path jarPath = DRIVER_DIR.resolve(driverJarFile);
        if (!Files.exists(jarPath)) {
            throw new java.io.FileNotFoundException("驱动JAR不存在: " + jarPath);
        }

        String cacheKey = connId + "_" + driverJarFile;
        URLClassLoader loader = classLoaderCache.computeIfAbsent(cacheKey, k -> {
            try {
                return new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, ClassLoader.getSystemClassLoader());
            } catch (Exception e) {
                throw new RuntimeException("创建 ClassLoader 失败", e);
            }
        });

        Class<?> driverClass = Class.forName(driverClassName, true, loader);
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();

        Properties props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        return driver.connect(url, props);
    }

    public void evictCache(String connId) {
        classLoaderCache.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(connId + "_")) {
                try {
                    entry.getValue().close();
                } catch (IOException e) {
                    log.warn("关闭 ClassLoader 失败: {}", e.getMessage());
                }
                return true;
            }
            return false;
        });
    }
}
