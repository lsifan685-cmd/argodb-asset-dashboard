# ArgoDB 轻量资产数据驾驶舱

轻量级 ArgoDB 数据资产全景大屏。配置 JDBC 连接后，自动生成包含 8 项核心 KPI + 9 个可视化图表的数据治理驾驶舱。

## 功能

- **连接管理**：多连接配置，动态加载 Argodb JDBC 驱动
- **元数据采集**：基于 ArgoDB system 视图 2 条 SQL 完成全库元数据采集（比逐表 DESC 快 ~10x）
- **资产大屏**：数据库/表/列/用户/权限统计，模型健康分，注释率，存储格式分布等
- **治理建议**：自动检测空表、宽表、缺失注释/归属等问题并生成建议

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7, Java 8+ |
| 前端 | 单页 HTML + ECharts 5 + Axios (CDN) |
| 存储 | JSON 文件 (data/connections.json) |
| 驱动 | URLClassLoader 动态加载 JDBC JAR |

## 快速开始

```bash
# 1. 构建
mvn package -DskipTests

# 2. 启动
java -jar target/argodb-asset-dashboard-1.0.0.jar

# 3. 打开浏览器
# http://localhost:8080
```

然后在页面中上传 ArgoDB JDBC 驱动 JAR，配置连接信息即可查看资产大屏。

## 项目结构

```
src/main/java/com/cc/argodb/dashboard/
├── DashboardApplication.java
├── config/
│   └── WebConfig.java
├── controller/
│   ├── ConnectionController.java
│   ├── DriverController.java
│   └── MetadataController.java
├── model/
│   ├── ApiResponse.java
│   ├── AssetStats.java
│   ├── ConnectionConfig.java
│   └── DatabaseTree.java (含 Database/Schema/Table/ColumnMetadata)
├── service/
│   ├── ConnectionService.java
│   ├── DriverService.java
│   ├── MetadataService.java        # 元数据采集（3 策略降级）
│   └── SystemTableService.java     # system 视图扩展统计
└── storage/
    └── JsonFileStorage.java
```

## API

### 连接管理
- `GET    /api/connections` — 列表
- `POST   /api/connections` — 新建
- `PUT    /api/connections/{id}` — 更新
- `DELETE /api/connections/{id}` — 删除
- `POST   /api/connections/test` — 连通性测试

### 元数据查询
- `GET /api/metadata/{connId}/full-tree` — 完整元数据树
- `GET /api/metadata/{connId}/stats` — 资产统计（主接口）

### 驱动管理
- `POST /api/drivers/upload` — 上传 JAR
- `GET  /api/drivers` — 驱动列表

### 效果示意
- 配置数据源 仅需 ip 端口 用户 密码 和驱动程序
<img width="2878" height="1610" alt="image" src="https://github.com/user-attachments/assets/103771e1-862e-4d21-a9e7-18c1fcb744da" />
- 等待10秒即可呈现数据资产情况
<img width="2878" height="1620" alt="image" src="https://github.com/user-attachments/assets/702d3755-7de1-49a1-beb0-2eb54cd55e92" />

