package com.cc.argodb.dashboard.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class JsonFileStorage {

    private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir"));
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonFileStorage() {
        try {
            Files.createDirectories(BASE_DIR.resolve("data"));
        } catch (IOException e) {
            throw new RuntimeException("无法创建 data 目录", e);
        }
    }

    public <T> List<T> readList(String filePath, TypeReference<List<T>> typeRef) {
        Path path = BASE_DIR.resolve(filePath);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                return new ArrayList<>();
            }
            return mapper.readValue(new String(bytes, StandardCharsets.UTF_8), typeRef);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + path, e);
        }
    }

    public <T> void writeList(String filePath, List<T> list) {
        Path path = BASE_DIR.resolve(filePath);
        try {
            Files.createDirectories(path.getParent());
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + path, e);
        }
    }
}
