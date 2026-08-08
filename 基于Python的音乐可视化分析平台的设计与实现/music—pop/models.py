"""
数据库模型
"""
from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin
from datetime import datetime, timedelta

db = SQLAlchemy()

class User(UserMixin, db.Model):
    """用户模型"""
    __tablename__ = 'users'
    
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(50), unique=True, nullable=False)
    email = db.Column(db.String(100), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    is_admin = db.Column(db.Boolean, default=False)
    is_vip = db.Column(db.Boolean, default=False)
    vip_expire_date = db.Column(db.DateTime, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    last_login = db.Column(db.DateTime, nullable=True)
    
    favorites = db.relationship('Favorite', backref='user', lazy='dynamic')
    play_history = db.relationship('PlayHistory', backref='user', lazy='dynamic')
    logs = db.relationship('UserLog', backref='user', lazy='dynamic')
    
    def is_vip_active(self):
        """检查VIP是否有效"""
        if not self.is_vip:
            return False
        if self.vip_expire_date is None:
            return False
        return datetime.utcnow() < self.vip_expire_date
    
    def get_favorite_genres(self):
        """获取用户喜好的音乐类型"""
        genres = {}
        for history in self.play_history.all():
            genre = history.genre or '未知'
            genres[genre] = genres.get(genre, 0) + 1
        return sorted(genres.items(), key=lambda x: x[1], reverse=True)

class Favorite(db.Model):
    """收藏模型"""
    __tablename__ = 'favorites'
    
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    song_id = db.Column(db.String(100), nullable=False)
    song_name = db.Column(db.String(200), nullable=False)
    artist = db.Column(db.String(200), nullable=False)
    cover_url = db.Column(db.String(500), nullable=True)
    genre = db.Column(db.String(50), nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    __table_args__ = (db.UniqueConstraint('user_id', 'song_id'),)

class PlayHistory(db.Model):
    """播放历史模型"""
    __tablename__ = 'play_history'
    
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    song_id = db.Column(db.String(100), nullable=False)
    song_name = db.Column(db.String(200), nullable=False)
    artist = db.Column(db.String(200), nullable=False)
    genre = db.Column(db.String(50), nullable=True)
    played_at = db.Column(db.DateTime, default=datetime.utcnow)
    play_duration = db.Column(db.Integer, default=0)

class UserLog(db.Model):
    """用户日志模型"""
    __tablename__ = 'user_logs'
    
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    action = db.Column(db.String(50), nullable=False)
    detail = db.Column(db.Text, nullable=True)
    ip_address = db.Column(db.String(50), nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

class SongStats(db.Model):
    """歌曲统计模型"""
    __tablename__ = 'song_stats'
    
    id = db.Column(db.Integer, primary_key=True)
    song_id = db.Column(db.String(100), unique=True, nullable=False)
    song_name = db.Column(db.String(200), nullable=False)
    artist = db.Column(db.String(200), nullable=False)
    genre = db.Column(db.String(50), nullable=True)
    play_count = db.Column(db.Integer, default=0)
    favorite_count = db.Column(db.Integer, default=0)
    last_played = db.Column(db.DateTime, nullable=True)

class VIPOrder(db.Model):
    """VIP订单模型"""
    __tablename__ = 'vip_orders'
    
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    duration_days = db.Column(db.Integer, nullable=False)
    amount = db.Column(db.Float, nullable=False)
    status = db.Column(db.String(20), default='pending')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    user = db.relationship('User', backref='vip_orders')
