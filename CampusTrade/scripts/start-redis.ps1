# 启动本地便携版 Redis（Windows）
# 若本机没有 Redis，项目会自动降级为进程内缓存，功能仍可用；
# 本脚本仅用于启用真正的 Redis 缓存。

$ErrorActionPreference = 'Stop'
$dir = Join-Path $env:LOCALAPPDATA 'CampusTrade\redis'
$exe = Join-Path $dir 'redis-server.exe'

if (-not (Test-Path $exe)) {
    Write-Host "未找到 Redis：$exe" -ForegroundColor Yellow
    Write-Host "可下载 Windows 版 Redis：https://github.com/tporadowski/redis/releases"
    Write-Host "解压后把 redis-server.exe 等文件放到：$dir"
    Write-Host "提示：不安装 Redis 也能正常使用，后端会自动降级为本地内存缓存。"
    exit 1
}

$running = Test-NetConnection -ComputerName 127.0.0.1 -Port 6379 -InformationLevel Quiet -WarningAction SilentlyContinue
if ($running) {
    Write-Host 'Redis 已在运行：127.0.0.1:6379' -ForegroundColor Green
    exit 0
}

Start-Process -FilePath $exe -ArgumentList (Join-Path $dir 'redis.windows.conf') -WorkingDirectory $dir -WindowStyle Hidden
Start-Sleep -Seconds 2
& (Join-Path $dir 'redis-cli.exe') PING
Write-Host 'Redis 启动完成：127.0.0.1:6379' -ForegroundColor Green