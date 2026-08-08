# 音乐平台系统 CentOS 7 部署指南

## 服务器信息
- **IP地址**: 192.168.10.133
- **操作系统**: CentOS 7
- **部署目录**: /var/www/music_platform

## 一、准备工作

### 1.1 上传项目文件到服务器
在本地 Windows 上执行（使用 scp 或其他工具）：
```bash
# 使用 scp 上传（需要安装 OpenSSH）
scp -r "d:\桌面\肖顺民毕设\music—pop" root@192.168.10.133:/tmp/
```

或者使用 FileZilla、WinSCP 等工具上传整个 `music—pop` 目录到服务器 `/tmp/` 目录。

## 二、服务器部署步骤

### 2.1 SSH 连接到服务器
```bash
ssh root@192.168.10.133
```

### 2.2 安装系统依赖
```bash
# 安装 EPEL 源
yum install -y epel-release

# 安装 Python3 和必要工具
yum install -y python3 python3-pip python3-devel gcc nginx

# 验证安装
python3 --version
pip3 --version
```

### 2.3 创建应用用户和目录
```bash
# 创建应用用户
useradd -r -s /sbin/nologin www

# 创建应用目录
mkdir -p /var/www/music_platform
mkdir -p /var/www/music_platform/logs
mkdir -p /var/www/music_platform/instance
```

### 2.4 复制应用文件
```bash
# 复制项目文件
cp /tmp/music—pop/app.py /var/www/music_platform/
cp /tmp/music—pop/models.py /var/www/music_platform/
cp /tmp/music—pop/requirements.txt /var/www/music_platform/
cp -r /tmp/music—pop/templates /var/www/music_platform/

# 复制部署配置
cp /tmp/music—pop/deploy/gunicorn_config.py /var/www/music_platform/
```

### 2.5 创建虚拟环境并安装依赖
```bash
cd /var/www/music_platform

# 创建虚拟环境
python3 -m venv venv

# 激活虚拟环境
source venv/bin/activate

# 升级 pip
pip install --upgrade pip

# 安装依赖
pip install -r requirements.txt
pip install gunicorn

# 退出虚拟环境
deactivate
```

### 2.6 设置目录权限
```bash
chown -R www:www /var/www/music_platform
chmod -R 755 /var/www/music_platform
```

### 2.7 配置 Systemd 服务
```bash
# 复制服务文件
cp /tmp/music—pop/deploy/music_platform.service /etc/systemd/system/

# 重载 systemd
systemctl daemon-reload

# 启用并启动服务
systemctl enable music_platform
systemctl start music_platform

# 检查状态
systemctl status music_platform
```

### 2.8 配置 Nginx
```bash
# 复制 Nginx 配置
cp /tmp/music—pop/deploy/nginx_music_platform.conf /etc/nginx/conf.d/

# 测试配置
nginx -t

# 启用并启动 Nginx
systemctl enable nginx
systemctl start nginx
```

### 2.9 配置防火墙
```bash
# 开放 HTTP 端口
firewall-cmd --permanent --add-service=http
firewall-cmd --reload

# 验证
firewall-cmd --list-all
```

### 2.10 关闭 SELinux（如果遇到权限问题）
```bash
# 临时关闭
setenforce 0

# 永久关闭（需重启生效）
sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config
```

## 三、验证部署

### 3.1 检查服务状态
```bash
# 检查应用服务
systemctl status music_platform

# 检查 Nginx
systemctl status nginx

# 查看应用日志
tail -f /var/www/music_platform/logs/error.log
```

### 3.2 访问测试
在浏览器中访问：
- **用户端**: http://192.168.10.133
- **管理员登录**: admin / admin123

## 四、常用管理命令

```bash
# 重启应用
systemctl restart music_platform

# 停止应用
systemctl stop music_platform

# 查看日志
journalctl -u music_platform -f

# 重启 Nginx
systemctl restart nginx

# 查看 Nginx 日志
tail -f /var/log/nginx/music_platform_error.log
```

## 五、故障排查

### 5.1 应用无法启动
```bash
# 检查日志
cat /var/www/music_platform/logs/stderr.log

# 手动测试运行
cd /var/www/music_platform
source venv/bin/activate
python app.py
```

### 5.2 无法访问网站
```bash
# 检查端口监听
netstat -tlnp | grep -E '80|5000'

# 检查防火墙
firewall-cmd --list-all

# 检查 SELinux
getenforce
```

### 5.3 数据库问题
```bash
# 数据库位于
/var/www/music_platform/instance/music_platform.db

# 确保目录权限正确
chown -R www:www /var/www/music_platform/instance
```

## 六、快速部署脚本

如果想一键部署，在服务器上执行：
```bash
cd /tmp/music—pop/deploy
chmod +x deploy.sh
./deploy.sh
```

---

**部署完成后访问地址**: http://192.168.10.133

**默认管理员账号**: admin / admin123
