# Gunicorn 配置文件
# 音乐平台系统

import multiprocessing

# 绑定地址（0.0.0.0 允许外部访问）
bind = "0.0.0.0:5000"

# 工作进程数
workers = multiprocessing.cpu_count() * 2 + 1

# 工作模式
worker_class = "sync"

# 最大请求数（重启工作进程）
max_requests = 1000
max_requests_jitter = 50

# 超时设置
timeout = 30
graceful_timeout = 30
keepalive = 2

# 日志配置
accesslog = "/var/www/music_platform/logs/access.log"
errorlog = "/var/www/music_platform/logs/error.log"
loglevel = "info"

# 进程名
proc_name = "music_platform"

# 守护进程（由 systemd 管理，设为 False）
daemon = False

# 预加载应用
preload_app = True
