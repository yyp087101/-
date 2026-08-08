#!/bin/bash
# ============================================
# 音乐平台系统 CentOS 7 一键部署脚本
# 服务器IP: 192.168.10.133
# 使用方法: 
#   1. 上传整个 music—pop 目录到服务器
#   2. cd /path/to/music—pop
#   3. chmod +x deploy.sh
#   4. ./deploy.sh
# ============================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量
APP_NAME="music_platform"
APP_DIR="/var/www/music_platform"
VENV_DIR="$APP_DIR/venv"
APP_USER="www"
APP_GROUP="www"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}     音乐平台系统 - 一键部署脚本${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

# 检查是否为 root 用户
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}错误: 请使用 root 用户运行此脚本${NC}"
    echo "使用: sudo ./deploy.sh"
    exit 1
fi

# ==================== 步骤 1: 安装系统依赖 ====================
echo -e "${YELLOW}[1/9] 安装系统依赖...${NC}"
yum install -y epel-release > /dev/null 2>&1
yum install -y python3 python3-pip python3-devel gcc nginx net-tools > /dev/null 2>&1
echo -e "${GREEN}      ✓ 系统依赖安装完成${NC}"

# ==================== 步骤 2: 创建应用用户 ====================
echo -e "${YELLOW}[2/9] 创建应用用户...${NC}"
if id "$APP_USER" &>/dev/null; then
    echo -e "${GREEN}      ✓ 用户 $APP_USER 已存在${NC}"
else
    useradd -r -s /sbin/nologin $APP_USER
    echo -e "${GREEN}      ✓ 用户 $APP_USER 创建完成${NC}"
fi

# ==================== 步骤 3: 创建应用目录 ====================
echo -e "${YELLOW}[3/9] 创建应用目录...${NC}"
mkdir -p $APP_DIR
mkdir -p $APP_DIR/logs
mkdir -p $APP_DIR/instance
echo -e "${GREEN}      ✓ 目录创建完成${NC}"

# ==================== 步骤 4: 复制应用文件 ====================
echo -e "${YELLOW}[4/9] 复制应用文件...${NC}"
cp "$SCRIPT_DIR/app.py" $APP_DIR/
cp "$SCRIPT_DIR/models.py" $APP_DIR/
cp "$SCRIPT_DIR/requirements.txt" $APP_DIR/
cp -r "$SCRIPT_DIR/templates" $APP_DIR/

# 创建 gunicorn 配置
cat > $APP_DIR/gunicorn_config.py << 'EOF'
# Gunicorn 配置文件
import multiprocessing

# 绑定地址（0.0.0.0 允许外部访问）
bind = "0.0.0.0:5000"

# 工作进程数
workers = multiprocessing.cpu_count() * 2 + 1

# 工作模式
worker_class = "sync"

# 最大请求数
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

# 守护进程
daemon = False

# 预加载应用
preload_app = True
EOF

echo -e "${GREEN}      ✓ 应用文件复制完成${NC}"

# ==================== 步骤 5: 创建虚拟环境并安装依赖 ====================
echo -e "${YELLOW}[5/9] 创建虚拟环境并安装依赖...${NC}"
cd $APP_DIR

# 创建虚拟环境
python3 -m venv $VENV_DIR

# 激活虚拟环境并安装依赖
source $VENV_DIR/bin/activate
pip install --upgrade pip > /dev/null 2>&1
pip install setuptools wheel > /dev/null 2>&1
pip install -r requirements.txt > /dev/null 2>&1
pip install gunicorn > /dev/null 2>&1
deactivate

echo -e "${GREEN}      ✓ 虚拟环境和依赖安装完成${NC}"

# ==================== 步骤 6: 初始化数据库 ====================
echo -e "${YELLOW}[6/9] 初始化数据库...${NC}"
cd $APP_DIR
source $VENV_DIR/bin/activate

python3 << 'PYEOF'
from app import app, db
from flask_bcrypt import Bcrypt
from models import User

bcrypt = Bcrypt(app)

with app.app_context():
    # 创建所有表
    db.create_all()
    print("      数据库表已创建")
    
    # 创建管理员账号
    admin = User.query.filter_by(username='admin').first()
    if not admin:
        password_hash = bcrypt.generate_password_hash('admin123').decode('utf-8')
        admin = User(
            username='admin',
            email='admin@music.com',
            password_hash=password_hash,
            is_admin=True
        )
        db.session.add(admin)
        db.session.commit()
        print("      管理员账号已创建: admin / admin123")
    else:
        print("      管理员账号已存在")
PYEOF

deactivate
echo -e "${GREEN}      ✓ 数据库初始化完成${NC}"

# ==================== 步骤 7: 设置目录权限 ====================
echo -e "${YELLOW}[7/9] 设置目录权限...${NC}"
chown -R $APP_USER:$APP_GROUP $APP_DIR
chmod -R 755 $APP_DIR
echo -e "${GREEN}      ✓ 权限设置完成${NC}"

# ==================== 步骤 8: 配置 systemd 服务 ====================
echo -e "${YELLOW}[8/9] 配置系统服务...${NC}"

# 创建 systemd 服务文件
cat > /etc/systemd/system/music_platform.service << EOF
[Unit]
Description=Music Platform - Flask Application
After=network.target

[Service]
Type=simple
User=$APP_USER
Group=$APP_GROUP
WorkingDirectory=$APP_DIR
Environment="PATH=$VENV_DIR/bin"
ExecStart=$VENV_DIR/bin/gunicorn -c gunicorn_config.py app:app
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# 重载 systemd
systemctl daemon-reload
systemctl enable music_platform > /dev/null 2>&1
systemctl restart music_platform

echo -e "${GREEN}      ✓ 系统服务配置完成${NC}"

# ==================== 步骤 9: 配置防火墙 ====================
echo -e "${YELLOW}[9/9] 配置防火墙...${NC}"

# 检查 firewalld 是否运行
if systemctl is-active --quiet firewalld; then
    firewall-cmd --permanent --add-port=5000/tcp > /dev/null 2>&1
    firewall-cmd --permanent --add-service=http > /dev/null 2>&1
    firewall-cmd --reload > /dev/null 2>&1
    echo -e "${GREEN}      ✓ 防火墙配置完成${NC}"
else
    echo -e "${YELLOW}      ! 防火墙未运行，跳过配置${NC}"
fi

# 关闭 SELinux（临时）
if command -v setenforce &> /dev/null; then
    setenforce 0 2>/dev/null || true
    echo -e "${GREEN}      ✓ SELinux 已临时关闭${NC}"
fi

# ==================== 验证部署 ====================
echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}           部署完成！${NC}"
echo -e "${GREEN}==========================================${NC}"
echo ""

# 等待服务启动
sleep 3

# 检查服务状态
if systemctl is-active --quiet music_platform; then
    echo -e "${GREEN}✓ 应用服务运行正常${NC}"
else
    echo -e "${RED}✗ 应用服务启动失败，请检查日志${NC}"
    echo "  查看日志: journalctl -u music_platform -n 50"
fi

# 获取服务器 IP
SERVER_IP=$(hostname -I | awk '{print $1}')

echo ""
echo -e "${GREEN}访问地址:${NC}"
echo -e "  http://${SERVER_IP}:5000"
echo ""
echo -e "${GREEN}管理员账号:${NC}"
echo -e "  用户名: admin"
echo -e "  密码:   admin123"
echo ""
echo -e "${YELLOW}常用命令:${NC}"
echo "  查看状态: systemctl status music_platform"
echo "  重启服务: systemctl restart music_platform"
echo "  查看日志: tail -f $APP_DIR/logs/error.log"
echo ""
echo -e "${GREEN}==========================================${NC}"
