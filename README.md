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

## 一键启动

**Linux / macOS**

```bash
#!/bin/bash
# start.sh

echo "🚀 启动后端..."
cd backend && mvn spring-boot:run &
BACKEND_PID=$!

echo "🚀 启动前端..."
cd ../frontend && npm run dev &
FRONTEND_PID=$!

# 等待 Ctrl+C
trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT TERM
wait
```

使用方式：

```bash
chmod +x start.sh
./start.sh
```

**Windows (PowerShell)**

```powershell
# start.ps1

Write-Host "🚀 启动后端..."
Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -WorkingDirectory "backend"

Write-Host "🚀 启动前端..."
Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory "frontend"
```

## 端口说明

| 服务 | 默认端口 | 配置文件 |
|------|----------|----------|
| 后端 | 8081 | `backend/src/main/resources/application.yml` |
| 前端 | 3000 | `frontend/vite.config.js` |

如遇端口冲突，修改对应配置文件中的端口号即可。
