#!/bin/bash
# ============================================
# 音乐平台系统 CentOS 7 部署脚本
# 服务器IP: 192.168.10.133
# ============================================

set -e

echo "=========================================="
echo "开始部署音乐平台系统"
echo "=========================================="

# 配置变量
APP_NAME="music_platform"
APP_DIR="/var/www/music_platform"
VENV_DIR="$APP_DIR/venv"
USER="www"
GROUP="www"

# 1. 安装系统依赖
echo "[1/8] 安装系统依赖..."
yum install -y epel-release
yum install -y python3 python3-pip python3-devel gcc nginx

# 2. 创建应用用户
echo "[2/8] 创建应用用户..."
id -u $USER &>/dev/null || useradd -r -s /sbin/nologin $USER

# 3. 创建应用目录
echo "[3/8] 创建应用目录..."
mkdir -p $APP_DIR
mkdir -p $APP_DIR/logs

# 4. 复制应用文件（假设当前目录是项目根目录）
echo "[4/8] 复制应用文件..."
cp -r app.py models.py templates requirements.txt $APP_DIR/
cp deploy/gunicorn_config.py $APP_DIR/

# 5. 创建虚拟环境并安装依赖
echo "[5/8] 创建虚拟环境并安装依赖..."
python3 -m venv $VENV_DIR
source $VENV_DIR/bin/activate
pip install --upgrade pip
pip install -r $APP_DIR/requirements.txt
pip install gunicorn
deactivate

# 6. 设置权限
echo "[6/8] 设置目录权限..."
chown -R $USER:$GROUP $APP_DIR
chmod -R 755 $APP_DIR

# 7. 配置 systemd 服务
echo "[7/8] 配置 systemd 服务..."
cp deploy/music_platform.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable music_platform
systemctl start music_platform

# 8. 配置 Nginx
echo "[8/8] 配置 Nginx..."
cp deploy/nginx_music_platform.conf /etc/nginx/conf.d/
nginx -t
systemctl enable nginx
systemctl restart nginx

# 配置防火墙
echo "配置防火墙..."
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-port=5000/tcp
firewall-cmd --reload

echo "=========================================="
echo "部署完成!"
echo "访问地址: http://192.168.10.133"
echo "管理员账号: admin / admin123"
echo "=========================================="
