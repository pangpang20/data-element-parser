#!/usr/bin/env bash
# 启动前后端服务
# 用法: ./start.sh
# 按 Ctrl+C 同时停止前后端

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

BACKEND_PORT=8081
FRONTEND_PORT=3000

# ---------- 工具函数 ----------

log() { echo -e "\033[1;32m[start]\033[0m $*"; }
err() { echo -e "\033[1;31m[start]\033[0m $*" >&2; }

# 检查端口是否被占用，占用则提示并退出
check_port() {
    local port=$1 name=$2
    if ss -tlnp | grep -q ":${port} "; then
        err "端口 $port 已被占用（$name 需要该端口）"
        err "请先停止占用该端口的进程，或修改配置"
        exit 1
    fi
}

# 清理：停止所有子进程
cleanup() {
    log "正在停止服务..."
    [[ -n "$BACKEND_PID" ]]  && kill "$BACKEND_PID"  2>/dev/null
    [[ -n "$FRONTEND_PID" ]] && kill "$FRONTEND_PID" 2>/dev/null
    wait 2>/dev/null
    log "已停止所有服务"
}
trap cleanup EXIT INT TERM

# ---------- 环境检查 ----------

check_port $BACKEND_PORT  "后端"
check_port $FRONTEND_PORT "前端"

# ---------- 启动后端 ----------

log "启动后端 (Spring Boot :$BACKEND_PORT)..."
cd "$BACKEND_DIR"
nohup mvn spring-boot:run -q \
    > "$PROJECT_DIR/backend.log" 2>&1 &
BACKEND_PID=$!

# 等待后端启动
log "等待后端就绪..."
for i in $(seq 1 30); do
    if curl -sf "http://localhost:$BACKEND_PORT/actuator/health" >/dev/null 2>&1 \
       || curl -sf "http://localhost:$BACKEND_PORT/" >/dev/null 2>&1; then
        log "后端已就绪 ✓"
        break
    fi
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        err "后端启动失败，查看日志: $PROJECT_DIR/backend.log"
        exit 1
    fi
    sleep 2
done

# ---------- 启动前端 ----------

log "启动前端 (Vite :$FRONTEND_PORT)..."
cd "$FRONTEND_DIR"
nohup npm run dev -- --host \
    > "$PROJECT_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!

sleep 3
if kill -0 "$FRONTEND_PID" 2>/dev/null; then
    log "前端已就绪 ✓"
else
    err "前端启动失败，查看日志: $PROJECT_DIR/frontend.log"
    exit 1
fi

# ---------- 完成 ----------

echo ""
log "=============================="
log "  后端: http://localhost:$BACKEND_PORT"
log "  前端: http://localhost:$FRONTEND_PORT"
log "  日志: backend.log / frontend.log"
log "  按 Ctrl+C 停止所有服务"
log "=============================="
echo ""

# 保持前台运行，等待信号
wait
