package com.cc.argodb.dashboard.service;

import com.cc.argodb.dashboard.model.ConnectionConfig;
import com.cc.argodb.dashboard.storage.JsonFileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class ConnectionService {

    private static final String STORAGE_FILE = "data/connections.json";

    private final JsonFileStorage storage;

    public ConnectionService(JsonFileStorage storage) {
        this.storage = storage;
    }

    @PostConstruct
    public void init() {
        List<ConnectionConfig> list = findAll();
        if (list.isEmpty()) {
            storage.writeList(STORAGE_FILE, list);
        }
    }

    public List<ConnectionConfig> findAll() {
        List<ConnectionConfig> list = storage.readList(STORAGE_FILE, new TypeReference<List<ConnectionConfig>>() {});
        for (ConnectionConfig c : list) {
            c.setPassword(null);
        }
        return list;
    }

    public ConnectionConfig findById(String id) {
        return storage.readList(STORAGE_FILE, new TypeReference<List<ConnectionConfig>>() {})
                .stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public ConnectionConfig save(ConnectionConfig input) {
        List<ConnectionConfig> list = storage.readList(STORAGE_FILE, new TypeReference<List<ConnectionConfig>>() {});
        String id = input.getId();
        if (id == null || id.isEmpty()) {
            id = "conn_" + System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        long createdAt = input.getCreatedAt() != 0 ? input.getCreatedAt() : now;

        ConnectionConfig config = new ConnectionConfig();
        config.setId(id);
        config.setName(input.getName());
        config.setUrl(input.getUrl());
        config.setDriverClassName(input.getDriverClassName());
        config.setUsername(input.getUsername());
        config.setPassword(input.getPassword());
        config.setDriverJarFile(input.getDriverJarFile());
        config.setCreatedAt(createdAt);
        config.setUpdatedAt(now);

        list.removeIf(c -> c.getId().equals(config.getId()));
        list.add(config);
        storage.writeList(STORAGE_FILE, list);
        return config;
    }

    public void delete(String id) {
        List<ConnectionConfig> list = storage.readList(STORAGE_FILE, new TypeReference<List<ConnectionConfig>>() {});
        list.removeIf(c -> c.getId().equals(id));
        storage.writeList(STORAGE_FILE, list);
    }
}
