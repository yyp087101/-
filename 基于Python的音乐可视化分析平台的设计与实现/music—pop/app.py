"""
音乐平台系统 - 主应用入口
"""
import os
# 禁用系统代理，确保API请求直连
os.environ['NO_PROXY'] = '*'
os.environ['no_proxy'] = '*'
if 'HTTP_PROXY' in os.environ:
    del os.environ['HTTP_PROXY']
if 'HTTPS_PROXY' in os.environ:
    del os.environ['HTTPS_PROXY']
if 'http_proxy' in os.environ:
    del os.environ['http_proxy']
if 'https_proxy' in os.environ:
    del os.environ['https_proxy']

import requests
import time
from datetime import datetime, timedelta
from functools import wraps
from flask import Flask, render_template, request, redirect, url_for, flash, jsonify, session

# 简单的内存缓存
class SimpleCache:
    def __init__(self, ttl=300):  # 默认5分钟过期
        self.cache = {}
        self.ttl = ttl
    
    def get(self, key):
        if key in self.cache:
            data, timestamp = self.cache[key]
            if time.time() - timestamp < self.ttl:
                return data
            del self.cache[key]
        return None
    
    def set(self, key, value):
        self.cache[key] = (value, time.time())
    
    def clear(self):
        self.cache = {}

# 创建缓存实例
search_cache = SimpleCache(ttl=600)  # 搜索结果缓存10分钟
song_cache = SimpleCache(ttl=1800)   # 歌曲信息缓存30分钟
cover_cache = SimpleCache(ttl=3600)  # 封面缓存1小时
from flask_login import LoginManager, login_user, logout_user, login_required, current_user
from flask_bcrypt import Bcrypt
from models import db, User, Favorite, PlayHistory, UserLog, SongStats, VIPOrder

app = Flask(__name__)
app.config['SECRET_KEY'] = 'music-platform-secret-key-2024'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///music_platform.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db.init_app(app)
bcrypt = Bcrypt(app)
login_manager = LoginManager(app)
login_manager.login_view = 'login'
login_manager.login_message = '请先登录'

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

def admin_required(f):
    """管理员权限装饰器"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not current_user.is_authenticated or not current_user.is_admin:
            flash('需要管理员权限', 'error')
            return redirect(url_for('index'))
        return f(*args, **kwargs)
    return decorated_function

def log_action(action, detail=None):
    """记录用户操作日志"""
    if current_user.is_authenticated:
        log = UserLog(
            user_id=current_user.id,
            action=action,
            detail=detail,
            ip_address=request.remote_addr
        )
        db.session.add(log)
        db.session.commit()

# ==================== 网易云音乐API集成 ====================

def search_music(keyword, page=1, limit=30):
    """搜索音乐 - 使用网易云音乐API（带缓存）"""
    # 检查缓存
    cache_key = f"{keyword}_{page}_{limit}"
    cached = search_cache.get(cache_key)
    if cached:
        return cached
    
    try:
        url = "https://music.163.com/api/search/get"
        # 多请求一些，因为要过滤付费歌曲
        data = {'s': keyword, 'type': 1, 'offset': (page-1)*limit, 'limit': limit * 3}
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://music.163.com/',
            'Content-Type': 'application/x-www-form-urlencoded',
        }
        resp = requests.post(url, data=data, headers=headers, timeout=15)
        result = resp.json()
        
        songs = []
        song_ids = []
        if result.get('code') == 200 and result.get('result', {}).get('songs'):
            for song in result['result']['songs']:
                # 过滤付费歌曲: fee=0免费, fee=1/4/8付费
                fee = song.get('fee', 0)
                if fee != 0:
                    continue
                    
                song_id = str(song.get('id', ''))
                songs.append({
                    'id': song_id,
                    'name': song.get('name', ''),
                    'artist': ' / '.join([a.get('name', '') for a in song.get('artists', [])]) or '未知歌手',
                    'album': song.get('album', {}).get('name', ''),
                    'cover': '',
                    'duration': song.get('duration', 0) // 1000,
                    'album_id': ''
                })
                song_ids.append(song_id)
                
                if len(songs) >= limit:
                    break
            
            # 批量获取封面
            if song_ids:
                covers = get_song_covers(song_ids)
                for song in songs:
                    song['cover'] = covers.get(song['id'], '')
        
        result_data = {'songs': songs, 'total': len(songs)}
        # 存入缓存
        search_cache.set(cache_key, result_data)
        return result_data
    except Exception as e:
        print(f"搜索失败: {e}")
    return {'songs': [], 'total': 0}

def get_song_covers(song_ids):
    """批量获取歌曲封面 - 带缓存"""
    if not song_ids:
        return {}
    
    # 检查缓存，获取已缓存的封面
    covers = {}
    uncached_ids = []
    for sid in song_ids:
        cached = cover_cache.get(sid)
        if cached:
            covers[sid] = cached
        else:
            uncached_ids.append(sid)
    
    # 如果所有封面都已缓存，直接返回
    if not uncached_ids:
        return covers
    
    try:
        ids_str = ','.join(uncached_ids)
        url = f"https://music.163.com/api/song/detail/?ids=[{ids_str}]"
        headers = {'User-Agent': 'Mozilla/5.0'}
        resp = requests.get(url, headers=headers, timeout=10)
        data = resp.json()
        if data.get('code') == 200 and data.get('songs'):
            for song in data['songs']:
                sid = str(song['id'])
                cover_url = song.get('album', {}).get('picUrl', '')
                covers[sid] = cover_url
                # 存入缓存
                cover_cache.set(sid, cover_url)
        return covers
    except Exception as e:
        print(f"获取封面失败: {e}")
    return covers

def get_song_url(song_id):
    """获取歌曲播放地址"""
    return f"/api/audio/{song_id}"

def get_playable_url(song_id):
    """获取可播放的音频URL"""
    # 直接使用网易云外链
    return f"https://music.163.com/song/media/outer/url?id={song_id}.mp3"

def get_song_detail(song_id):
    """获取歌曲详情"""
    return None

def get_lyrics(song_id):
    """获取歌词"""
    try:
        url = f"http://music.163.com/api/song/lyric?id={song_id}&lv=1"
        headers = {'User-Agent': 'Mozilla/5.0'}
        resp = requests.get(url, headers=headers, timeout=10)
        data = resp.json()
        if data.get('code') == 200 and data.get('lrc'):
            return data['lrc'].get('lyric', '')
    except:
        pass
    return ''

def get_mv_url(song_id):
    """获取MV地址"""
    try:
        url = f"https://music.163.com/api/song/detail/?ids=[{song_id}]"
        headers = {'User-Agent': 'Mozilla/5.0'}
        resp = requests.get(url, headers=headers, timeout=10)
        data = resp.json()
        if data.get('code') == 200 and data.get('songs'):
            mv_id = data['songs'][0].get('mvid', 0)
            if mv_id:
                mv_url = f"https://music.163.com/api/mv/detail?id={mv_id}"
                mv_resp = requests.get(mv_url, headers=headers, timeout=10)
                mv_data = mv_resp.json()
                if mv_data.get('code') == 200:
                    return mv_data.get('data', {}).get('brs', {}).get('480', '')
    except:
        pass
    return None

def get_recommend_songs(genre=None, limit=20):
    """获取推荐歌曲"""
    try:
        url = "https://music.163.com/api/personalized/newsong"
        headers = {'User-Agent': 'Mozilla/5.0'}
        resp = requests.get(url, headers=headers, timeout=10)
        data = resp.json()
        if data.get('code') == 200:
            songs = []
            for item in data.get('result', [])[:limit]:
                song = item.get('song', {})
                songs.append({
                    'id': str(song.get('id', '')),
                    'name': song.get('name', ''),
                    'artist': '/'.join([a.get('name', '') for a in song.get('artists', [])]),
                    'album': song.get('album', {}).get('name', ''),
                    'cover': song.get('album', {}).get('picUrl', ''),
                })
            return songs
    except Exception as e:
        print(f"获取推荐失败: {e}")
    return []

def get_hot_songs(limit=20):
    """获取热门歌曲 - 通过搜索热门关键词获取免费歌曲"""
    try:
        # 使用搜索API搜索热门关键词，获取免费歌曲
        keywords = ['经典老歌', '华语流行', '民谣', '轻音乐']
        all_songs = []
        
        for keyword in keywords:
            if len(all_songs) >= limit:
                break
            result = search_music(keyword, page=1, limit=10)
            for song in result.get('songs', []):
                if song not in all_songs:
                    all_songs.append(song)
                    if len(all_songs) >= limit:
                        break
        
        return all_songs[:limit]
    except Exception as e:
        print(f"获取热门失败: {e}")
    return []

# ==================== 路由 ====================

@app.route('/')
def index():
    """首页"""
    if current_user.is_authenticated:
        return redirect(url_for('user_home'))
    return render_template('index.html')

@app.route('/register', methods=['GET', 'POST'])
def register():
    """用户注册"""
    if current_user.is_authenticated:
        return redirect(url_for('user_home'))
    
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        email = request.form.get('email', '').strip()
        password = request.form.get('password', '')
        
        if not username or not email or not password:
            flash('请填写所有字段', 'error')
            return render_template('register.html')
        
        if User.query.filter_by(username=username).first():
            flash('用户名已存在', 'error')
            return render_template('register.html')
        
        if User.query.filter_by(email=email).first():
            flash('邮箱已被注册', 'error')
            return render_template('register.html')
        
        password_hash = bcrypt.generate_password_hash(password).decode('utf-8')
        user = User(username=username, email=email, password_hash=password_hash)
        db.session.add(user)
        db.session.commit()
        
        flash('注册成功，请登录', 'success')
        return redirect(url_for('login'))
    
    return render_template('register.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    """用户登录"""
    if current_user.is_authenticated:
        if current_user.is_admin:
            return redirect(url_for('admin_dashboard'))
        return redirect(url_for('user_home'))
    
    if request.method == 'POST':
        username = request.form.get('username', '').strip()
        password = request.form.get('password', '')
        
        user = User.query.filter_by(username=username).first()
        if user and bcrypt.check_password_hash(user.password_hash, password):
            login_user(user)
            user.last_login = datetime.utcnow()
            db.session.commit()
            log_action('登录', f'用户 {username} 登录成功')
            
            if user.is_admin:
                return redirect(url_for('admin_dashboard'))
            return redirect(url_for('user_home'))
        
        flash('用户名或密码错误', 'error')
    
    return render_template('login.html')

@app.route('/logout')
@login_required
def logout():
    """退出登录"""
    log_action('退出', f'用户 {current_user.username} 退出登录')
    logout_user()
    return redirect(url_for('index'))

# ==================== 用户端路由 ====================

@app.route('/home')
@login_required
def user_home():
    """用户首页 - 从API获取音乐数据"""
    mode = request.args.get('mode', 'default')
    
    # 热门歌曲：直接从API获取，确保封面图正常显示
    hot_songs = get_hot_songs(12)
    
    # 推荐歌曲：根据模式不同获取
    if mode == 'familiar':
        # 熟悉模式：获取用户听歌历史，批量获取封面（优化性能）
        user_history = PlayHistory.query.filter_by(user_id=current_user.id).order_by(PlayHistory.played_at.desc()).limit(12).all()
        song_ids = [h.song_id for h in user_history]
        covers = get_song_covers(song_ids)  # 批量获取封面
        recommend_songs = [{
            'id': h.song_id,
            'name': h.song_name,
            'artist': h.artist,
            'cover': covers.get(h.song_id, '')
        } for h in user_history]
        # 如果历史不足，从API获取热门歌曲补充
        if len(recommend_songs) < 12:
            api_recommend = get_hot_songs(12 - len(recommend_songs))
            recommend_songs.extend(api_recommend)
    elif mode == 'fresh':
        # 新鲜模式：从API获取新歌推荐，排除用户已听过的
        played_ids = set(h.song_id for h in PlayHistory.query.filter_by(user_id=current_user.id).all())
        api_fresh = get_recommend_songs(limit=20)
        recommend_songs = [s for s in api_fresh if s['id'] not in played_ids][:12]
        # 如果新歌不足，从热门中补充
        if len(recommend_songs) < 12:
            api_hot = get_hot_songs(20)
            for s in api_hot:
                if s['id'] not in played_ids and s not in recommend_songs:
                    recommend_songs.append(s)
                    if len(recommend_songs) >= 12:
                        break
    else:
        # 默认模式：从API获取热门歌曲
        recommend_songs = get_hot_songs(12)
        # 如果API失败，尝试从收藏获取
        if not recommend_songs:
            recent_favs = Favorite.query.order_by(Favorite.created_at.desc()).limit(12).all()
            recommend_songs = [{
                'id': f.song_id,
                'name': f.song_name,
                'artist': f.artist,
                'cover': f.cover_url or ''
            } for f in recent_favs]
    
    return render_template('user/home.html', 
                         hot_songs=hot_songs, 
                         recommend_songs=recommend_songs,
                         mode=mode,
                         is_vip=current_user.is_vip_active())

@app.route('/search')
@login_required
def search():
    """搜索页面"""
    keyword = request.args.get('q', '')
    page = request.args.get('page', 1, type=int)
    
    results = {'songs': [], 'total': 0}
    if keyword:
        results = search_music(keyword, page)
        log_action('搜索', f'搜索关键词: {keyword}')
    
    return render_template('user/search.html', 
                         keyword=keyword, 
                         results=results,
                         page=page)

@app.route('/play/<song_id>')
@login_required
def play_song(song_id):
    """播放歌曲页面 - 优化加载速度"""
    # 先从数据库查找歌曲信息
    stats = SongStats.query.filter_by(song_id=song_id).first()
    fav = Favorite.query.filter_by(song_id=song_id).first()
    
    # 从请求参数获取歌曲信息（从列表点击过来的）
    song_name = request.args.get('name', '')
    artist = request.args.get('artist', '')
    cover = request.args.get('cover', '')
    
    # 如果数据库有记录，使用数据库的信息
    if stats:
        song_name = song_name or stats.song_name
        artist = artist or stats.artist
    elif fav:
        song_name = song_name or fav.song_name
        artist = artist or fav.artist
        cover = cover or fav.cover_url
    
    # 如果封面为空，从网易云API获取
    if not cover:
        try:
            url = f"https://music.163.com/api/song/detail/?ids=[{song_id}]"
            headers = {'User-Agent': 'Mozilla/5.0'}
            resp = requests.get(url, headers=headers, timeout=5)
            data = resp.json()
            if data.get('code') == 200 and data.get('songs'):
                song_info = data['songs'][0]
                cover = song_info.get('album', {}).get('picUrl', '')
                if not song_name:
                    song_name = song_info.get('name', '')
                if not artist:
                    artist = '/'.join([a.get('name', '') for a in song_info.get('artists', [])])
        except Exception as e:
            print(f"获取歌曲详情失败: {e}")
    
    # 获取album_id参数
    album_id = request.args.get('album_id', '0')
    
    # 尝试获取可播放的音频URL
    audio_url = get_playable_url(song_id)
    
    # 构建歌曲对象
    song = {
        'id': song_id,
        'name': song_name or f'歌曲{song_id}',
        'artist': artist or '未知歌手',
        'album': '',
        'cover': cover,
        'url': audio_url
    }
    
    # 记录播放历史
    history = PlayHistory(
        user_id=current_user.id,
        song_id=song_id,
        song_name=song['name'],
        artist=song['artist']
    )
    db.session.add(history)
    
    # 更新歌曲统计
    if stats:
        stats.play_count += 1
        stats.last_played = datetime.utcnow()
    else:
        stats = SongStats(
            song_id=song_id,
            song_name=song['name'],
            artist=song['artist'],
            play_count=1,
            last_played=datetime.utcnow()
        )
        db.session.add(stats)
    
    db.session.commit()
    log_action('播放', f'播放歌曲: {song["name"]}')
    
    # 获取歌词
    lyrics = get_lyrics(song_id)
    
    return render_template('user/play.html', 
                         song=song, 
                         lyrics=lyrics,
                         mv_url=None,
                         is_vip=current_user.is_vip_active())

@app.route('/favorites')
@login_required
def favorites():
    """收藏列表"""
    favs = current_user.favorites.order_by(Favorite.created_at.desc()).all()
    return render_template('user/favorites.html', favorites=favs)

@app.route('/api/favorite/<song_id>', methods=['POST'])
@login_required
def toggle_favorite(song_id):
    """切换收藏状态"""
    data = request.get_json()
    existing = Favorite.query.filter_by(user_id=current_user.id, song_id=song_id).first()
    
    if existing:
        db.session.delete(existing)
        db.session.commit()
        log_action('取消收藏', f'取消收藏歌曲ID: {song_id}')
        return jsonify({'status': 'removed'})
    else:
        fav = Favorite(
            user_id=current_user.id,
            song_id=song_id,
            song_name=data.get('name', ''),
            artist=data.get('artist', ''),
            cover_url=data.get('cover', '')
        )
        db.session.add(fav)
        
        stats = SongStats.query.filter_by(song_id=song_id).first()
        if stats:
            stats.favorite_count += 1
        
        db.session.commit()
        log_action('收藏', f'收藏歌曲: {data.get("name", "")}')
        return jsonify({'status': 'added'})

@app.route('/api/check_favorite/<song_id>')
@login_required
def check_favorite(song_id):
    """检查是否已收藏"""
    existing = Favorite.query.filter_by(user_id=current_user.id, song_id=song_id).first()
    return jsonify({'is_favorite': existing is not None})

@app.route('/api/song_detail/<song_id>')
@login_required
def api_song_detail(song_id):
    """异步获取歌曲详情（用于补充信息）"""
    song = get_song_detail(song_id)
    if song:
        # 更新数据库中的歌曲信息
        stats = SongStats.query.filter_by(song_id=song_id).first()
        if stats and not stats.song_name:
            stats.song_name = song['name']
            stats.artist = song['artist']
            db.session.commit()
        return jsonify(song)
    return jsonify({'error': 'not found'}), 404

@app.route('/api/mv_url/<song_id>')
@login_required
def api_mv_url(song_id):
    """异步获取MV地址（VIP专用）"""
    if not current_user.is_vip_active():
        return jsonify({'error': 'VIP only'}), 403
    mv_url = get_mv_url(song_id)
    return jsonify({'mv_url': mv_url})

@app.route('/api/play_url/<song_id>')
def api_play_url(song_id):
    """获取歌曲播放地址"""
    try:
        # 尝试获取真实播放地址
        url = f"https://music.163.com/api/song/enhance/player/url?ids=[{song_id}]&br=320000"
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
            'Referer': 'https://music.163.com/'
        }
        resp = requests.get(url, headers=headers, timeout=10)
        data = resp.json()
        if data.get('code') == 200 and data.get('data'):
            for item in data['data']:
                if item.get('url'):
                    return jsonify({'url': item['url']})
    except Exception as e:
        print(f"获取播放地址失败: {e}")
    
    # 返回外链地址
    return jsonify({'url': f"https://music.163.com/song/media/outer/url?id={song_id}.mp3"})

@app.route('/api/audio/<path:song_id>')
def audio_proxy(song_id):
    """音频代理 - 直接重定向到网易云音乐外链"""
    # 直接重定向到网易云音乐外链，让浏览器直接请求
    play_url = f"http://music.163.com/song/media/outer/url?id={song_id}.mp3"
    return redirect(play_url)

@app.route('/visualizer')
@login_required
def visualizer():
    """音乐可视化分析页面"""
    return render_template('user/visualizer.html')

@app.route('/vip')
@login_required
def vip_page():
    """VIP会员页面"""
    return render_template('user/vip.html', is_vip=current_user.is_vip_active())

@app.route('/api/buy_vip', methods=['POST'])
@login_required
def buy_vip():
    """购买VIP"""
    data = request.get_json()
    days = data.get('days', 30)
    
    if current_user.is_vip_active():
        current_user.vip_expire_date += timedelta(days=days)
    else:
        current_user.is_vip = True
        current_user.vip_expire_date = datetime.utcnow() + timedelta(days=days)
    
    order = VIPOrder(
        user_id=current_user.id,
        duration_days=days,
        amount=days * 0.5,
        status='completed'
    )
    db.session.add(order)
    db.session.commit()
    
    log_action('购买VIP', f'购买 {days} 天VIP')
    return jsonify({'success': True, 'expire_date': current_user.vip_expire_date.strftime('%Y-%m-%d')})

# ==================== 管理员路由 ====================

@app.route('/admin')
@login_required
@admin_required
def admin_dashboard():
    """管理员控制台"""
    user_count = User.query.filter_by(is_admin=False).count()
    vip_count = User.query.filter(User.is_vip==True, User.vip_expire_date > datetime.utcnow()).count()
    total_plays = db.session.query(db.func.sum(SongStats.play_count)).scalar() or 0
    today_logs = UserLog.query.filter(UserLog.created_at >= datetime.utcnow().date()).count()
    
    top_songs = SongStats.query.order_by(SongStats.play_count.desc()).limit(10).all()
    recent_logs = UserLog.query.order_by(UserLog.created_at.desc()).limit(20).all()
    
    return render_template('admin/dashboard.html',
                         user_count=user_count,
                         vip_count=vip_count,
                         total_plays=total_plays,
                         today_logs=today_logs,
                         top_songs=top_songs,
                         recent_logs=recent_logs)

@app.route('/admin/users')
@login_required
@admin_required
def admin_users():
    """用户管理"""
    users = User.query.filter_by(is_admin=False).order_by(User.created_at.desc()).all()
    return render_template('admin/users.html', users=users)

@app.route('/admin/user/<int:user_id>/toggle_vip', methods=['POST'])
@login_required
@admin_required
def toggle_user_vip(user_id):
    """切换用户VIP状态"""
    user = User.query.get_or_404(user_id)
    data = request.get_json()
    days = data.get('days', 30)
    
    if user.is_vip_active():
        user.is_vip = False
        user.vip_expire_date = None
        action = '取消VIP'
    else:
        user.is_vip = True
        user.vip_expire_date = datetime.utcnow() + timedelta(days=days)
        action = f'开通VIP {days}天'
    
    db.session.commit()
    log_action('管理VIP', f'{action}: {user.username}')
    return jsonify({'success': True})

@app.route('/admin/user/<int:user_id>/delete', methods=['POST'])
@login_required
@admin_required
def delete_user(user_id):
    """删除用户"""
    user = User.query.get_or_404(user_id)
    if user.is_admin:
        return jsonify({'success': False, 'message': '不能删除管理员'})
    
    username = user.username
    db.session.delete(user)
    db.session.commit()
    log_action('删除用户', f'删除用户: {username}')
    return jsonify({'success': True})

@app.route('/admin/logs')
@login_required
@admin_required
def admin_logs():
    """日志管理"""
    page = request.args.get('page', 1, type=int)
    logs = UserLog.query.order_by(UserLog.created_at.desc()).paginate(page=page, per_page=50)
    return render_template('admin/logs.html', logs=logs)

@app.route('/admin/vip')
@login_required
@admin_required
def admin_vip():
    """VIP管理"""
    vip_users = User.query.filter(User.is_vip==True).order_by(User.vip_expire_date.desc()).all()
    orders = VIPOrder.query.order_by(VIPOrder.created_at.desc()).limit(50).all()
    return render_template('admin/vip.html', vip_users=vip_users, orders=orders)

@app.route('/admin/stats')
@login_required
@admin_required
def admin_stats():
    """数据统计 - 增强版"""
    today = datetime.utcnow().date()
    week_ago = today - timedelta(days=7)
    month_ago = today - timedelta(days=30)
    
    # ========== 核心指标 ==========
    total_users = User.query.filter_by(is_admin=False).count()
    total_vip = User.query.filter(User.is_vip==True, User.vip_expire_date > datetime.utcnow()).count()
    total_plays = db.session.query(db.func.sum(SongStats.play_count)).scalar() or 0
    total_favorites = Favorite.query.count()
    
    # 今日数据
    today_plays = PlayHistory.query.filter(db.func.date(PlayHistory.played_at) == today).count()
    today_users = User.query.filter(db.func.date(User.created_at) == today).count()
    today_active = db.session.query(db.func.count(db.distinct(PlayHistory.user_id))).filter(
        db.func.date(PlayHistory.played_at) == today
    ).scalar() or 0
    
    # 本周数据
    week_plays = PlayHistory.query.filter(db.func.date(PlayHistory.played_at) >= week_ago).count()
    week_users = User.query.filter(db.func.date(User.created_at) >= week_ago).count()
    
    # VIP转化率
    vip_rate = round(total_vip / total_users * 100, 1) if total_users > 0 else 0
    
    # ========== 热门歌曲 ==========
    top_songs = SongStats.query.order_by(SongStats.play_count.desc()).limit(10).all()
    
    # ========== 热门歌手 ==========
    top_artists_raw = db.session.query(
        SongStats.artist,
        db.func.sum(SongStats.play_count).label('total_plays'),
        db.func.count(SongStats.id).label('song_count')
    ).group_by(SongStats.artist).order_by(db.func.sum(SongStats.play_count).desc()).limit(10).all()
    top_artists = [{'name': row[0] or '未知', 'plays': row[1] or 0, 'songs': row[2]} for row in top_artists_raw]
    
    # ========== 音乐类型分布 ==========
    genre_stats_raw = db.session.query(
        PlayHistory.genre, 
        db.func.count(PlayHistory.id)
    ).group_by(PlayHistory.genre).all()
    genre_stats = [[row[0] or '未知', row[1]] for row in genre_stats_raw]
    
    # ========== 每日播放趋势（30天）==========
    daily_plays_raw = db.session.query(
        db.func.date(PlayHistory.played_at),
        db.func.count(PlayHistory.id)
    ).group_by(db.func.date(PlayHistory.played_at)).order_by(db.func.date(PlayHistory.played_at).desc()).limit(30).all()
    daily_plays = [[str(row[0]) if row[0] else '', row[1]] for row in daily_plays_raw]
    
    # ========== 用户注册趋势（30天）==========
    daily_users_raw = db.session.query(
        db.func.date(User.created_at),
        db.func.count(User.id)
    ).filter(User.is_admin == False).group_by(db.func.date(User.created_at)).order_by(db.func.date(User.created_at).desc()).limit(30).all()
    daily_users = [[str(row[0]) if row[0] else '', row[1]] for row in daily_users_raw]
    
    # ========== 播放时段分布（按小时）==========
    hourly_plays_raw = db.session.query(
        db.func.strftime('%H', PlayHistory.played_at),
        db.func.count(PlayHistory.id)
    ).group_by(db.func.strftime('%H', PlayHistory.played_at)).all()
    hourly_plays = [[int(row[0]) if row[0] else 0, row[1]] for row in hourly_plays_raw]
    hourly_plays.sort(key=lambda x: x[0])
    
    # ========== 活跃用户排行 ==========
    active_users_raw = db.session.query(
        User.username,
        db.func.count(PlayHistory.id).label('play_count')
    ).join(PlayHistory).group_by(User.id).order_by(db.func.count(PlayHistory.id).desc()).limit(10).all()
    active_users = [{'name': row[0], 'plays': row[1]} for row in active_users_raw]
    
    # ========== VIP收入统计 ==========
    total_revenue = db.session.query(db.func.sum(VIPOrder.amount)).filter(VIPOrder.status == 'completed').scalar() or 0
    month_revenue = db.session.query(db.func.sum(VIPOrder.amount)).filter(
        VIPOrder.status == 'completed',
        db.func.date(VIPOrder.created_at) >= month_ago
    ).scalar() or 0
    
    return render_template('admin/stats.html',
                         # 核心指标
                         total_users=total_users,
                         total_vip=total_vip,
                         total_plays=total_plays,
                         total_favorites=total_favorites,
                         today_plays=today_plays,
                         today_users=today_users,
                         today_active=today_active,
                         week_plays=week_plays,
                         week_users=week_users,
                         vip_rate=vip_rate,
                         # 排行榜
                         top_songs=top_songs,
                         top_artists=top_artists,
                         active_users=active_users,
                         # 图表数据
                         genre_stats=genre_stats,
                         daily_plays=daily_plays,
                         daily_users=daily_users,
                         hourly_plays=hourly_plays,
                         # 收入
                         total_revenue=total_revenue,
                         month_revenue=month_revenue)

@app.route('/admin/visualizer')
@login_required
@admin_required
def admin_visualizer():
    """管理员音乐可视化"""
    return render_template('admin/visualizer.html')

# ==================== 初始化 ====================

def init_db():
    """初始化数据库"""
    with app.app_context():
        db.create_all()
        
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
            print("管理员账号已创建: admin / admin123")

if __name__ == '__main__':
    init_db()
    print("=" * 50)
    print("音乐平台系统已启动")
    print("用户端: http://127.0.0.1:5000")
    print("管理员: admin / admin123")
    print("=" * 50)
    # 生产环境使用 gunicorn，开发环境使用 flask 内置服务器
    import os
    if os.environ.get('FLASK_ENV') == 'production':
        app.run(debug=False, host='0.0.0.0', port=5000)
    else:
        app.run(debug=True, host='127.0.0.1', port=5000)
