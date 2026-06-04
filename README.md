# Data Element Parser

数据元解析与治理规则生成服务 —— 基于 Spring Boot + Vue 3 的全栈应用。

## 环境要求

| 组件 | 版本要求 |
|------|----------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |

## 快速启动

### 后端（Spring Boot）

```bash
cd backend
mvn spring-boot:run
```

启动后监听 `http://localhost:8081`，API 前缀 `/api`。

### 前端（Vue 3 + Vite）

```bash
cd frontend
npm install    # 首次运行需安装依赖
npm run dev
```

启动后访问 `http://localhost:3000`（端口被占用时 Vite 会自动递增）。

前端已配置代理，`/api` 请求自动转发到后端 `http://localhost:8081`。

## 项目结构

```
.
├── backend/                    # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/java/
│       └── com/datagov/
│           ├── DataGovApplication.java   # 启动入口
│           ├── CorsConfig.java           # 跨域配置
│           ├── controller/               # REST 接口
│           ├── model/                    # 数据模型
│           └── service/                  # 业务逻辑
│
└── frontend/                   # Vue 3 前端
    ├── package.json
    ├── vite.config.js          # Vite 配置（含 API 代理）
    ├── index.html
    └── src/
```

## 常用命令

```bash
# 后端打包
cd backend && mvn clean package

# 前端构建
cd frontend && npm run build

# 前端预览构建产物
cd frontend && npm run preview
```
