#!/usr/bin/env bash
# 停止前后端服务
# 用法: ./stop.sh

log() { echo -e "\033[1;33m[stop]\033[0m $*"; }

BACKEND_PORT=8081
FRONTEND_PORT=3000

stopped=0

# 按端口查找并杀死进程
kill_by_port() {
    local port=$1 name=$2
    local pids
    pids=$(ss -tlnp | grep ":${port} " | grep -oP 'pid=\K[0-9]+' | sort -u)
    if [[ -n "$pids" ]]; then
        for pid in $pids; do
            log "停止 $name (PID: $pid, 端口: $port)"
            kill "$pid" 2>/dev/null
        done
        stopped=1
    fi
}

# 杀死 Maven 子进程（spring-boot:run 会 fork java 进程）
kill_maven() {
    local pids
    pids=$(pgrep -f "spring-boot:run" 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
        for pid in $pids; do
            log "停止 Maven 进程 (PID: $pid)"
            kill "$pid" 2>/dev/null
        done
        stopped=1
    fi
}

kill_by_port  $BACKEND_PORT  "后端"
kill_by_port  $FRONTEND_PORT "前端"
kill_maven

# 等待进程退出
sleep 2

# 强制清理残留
for port in $BACKEND_PORT $FRONTEND_PORT; do
    pids=$(ss -tlnp | grep ":${port} " | grep -oP 'pid=\K[0-9]+' | sort -u)
    if [[ -n "$pids" ]]; then
        for pid in $pids; do
            log "强制终止残留进程 (PID: $pid)"
            kill -9 "$pid" 2>/dev/null
        done
    fi
done

if [[ $stopped -eq 1 ]]; then
    log "已停止所有服务 ✓"
else
    log "没有发现运行中的服务"
fi
