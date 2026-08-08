-- 宠物领养管理系统数据库初始化脚本
CREATE DATABASE IF NOT EXISTS pet_adoption DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pet_adoption;

-- 用户表
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `adoption`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `pet`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `realname` VARCHAR(50) DEFAULT '' COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT '' COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT '' COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT '' COMMENT '头像路径',
    `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通用户, 1-管理员',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 宠物表
CREATE TABLE `pet` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '宠物名称',
    `type` VARCHAR(20) NOT NULL COMMENT '宠物类型',
    `breed` VARCHAR(50) DEFAULT '' COMMENT '品种',
    `age` VARCHAR(20) DEFAULT '' COMMENT '年龄',
    `gender` TINYINT DEFAULT 1 COMMENT '性别：0-母, 1-公',
    `color` VARCHAR(30) DEFAULT '' COMMENT '颜色',
    `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
    `health_status` VARCHAR(100) DEFAULT '健康' COMMENT '健康状况',
    `description` TEXT COMMENT '描述信息',
    `image` VARCHAR(255) DEFAULT '' COMMENT '宠物图片',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待领养, 1-已领养, 2-已下架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';

-- 领养申请表
CREATE TABLE `adoption` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '申请人ID',
    `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
    `reason` TEXT COMMENT '领养理由',
    `address` VARCHAR(255) DEFAULT '' COMMENT '居住地址',
    `experience` VARCHAR(255) DEFAULT '' COMMENT '养宠经验',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审核, 1-审核通过, 2-审核拒绝, 3-已取消',
    `review_comment` VARCHAR(255) DEFAULT '' COMMENT '审核意见',
    `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`pet_id`) REFERENCES `pet`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养申请表';

-- 公告表
CREATE TABLE `notice` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT COMMENT '公告内容',
    `publisher_id` BIGINT DEFAULT NULL COMMENT '发布人ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-下架, 1-发布',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`publisher_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 留言反馈表
CREATE TABLE `feedback` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) DEFAULT '' COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '留言内容',
    `reply` TEXT COMMENT '管理员回复',
    `reply_id` BIGINT DEFAULT NULL COMMENT '回复人ID',
    `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言反馈表';

-- 插入默认管理员账号（密码为MD5加密后的123456）
INSERT INTO `user` (`username`, `password`, `realname`, `role`, `status`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 1);

-- 插入测试用户
INSERT INTO `user` (`username`, `password`, `realname`, `phone`, `email`, `role`, `status`) VALUES
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', '13800138001', 'zhangsan@example.com', 0, 1),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', '13800138002', 'lisi@example.com', 0, 1);

-- 插入测试宠物数据
INSERT INTO `pet` (`name`, `type`, `breed`, `age`, `gender`, `color`, `weight`, `health_status`, `description`, `status`) VALUES
('小白', '猫', '英短', '2岁', 1, '白色', 4.5, '健康，已绝育已免疫', '性格温顺，喜欢被抚摸，适合家庭饲养。', 0),
('大黄', '狗', '金毛', '3岁', 1, '金色', 28.0, '健康，已免疫', '活泼好动，对人友善，喜欢户外运动。', 0),
('花花', '猫', '三花猫', '1岁', 0, '三花色', 3.2, '健康，已绝育', '可爱的三花猫，性格独立但亲人。', 0),
('旺财', '狗', '柴犬', '2岁', 1, '赤色', 10.5, '健康，已绝育已免疫', '标准的柴犬笑容，性格忠诚。', 0),
('咪咪', '猫', '橘猫', '1.5岁', 0, '橘色', 5.0, '健康，已免疫', '大橘为重，是个小吃货，性格温和。', 0),
('豆豆', '狗', '泰迪', '1岁', 1, '棕色', 5.5, '健康，已免疫', '聪明伶俐的小泰迪，不掉毛。', 0),
('小黑', '猫', '黑猫', '3岁', 1, '黑色', 4.0, '健康，已绝育已免疫', '神秘的黑色外表下有一颗温暖的心。', 0),
('球球', '仓鼠', '金丝熊', '半岁', 0, '棕白', 0.1, '健康', '圆滚滚的小仓鼠，活泼可爱。', 0);

-- 插入测试公告
INSERT INTO `notice` (`title`, `content`, `publisher_id`, `status`) VALUES
('欢迎使用宠物领养管理系统', '本系统致力于为流浪动物寻找温暖的家，如果您有爱心并且具备饲养条件，欢迎申请领养！', 1, 1),
('领养须知', '领养宠物前请确保：1.家人同意 2.有稳定住所 3.有经济能力承担宠物日常开销 4.愿意对宠物负责到底。', 1, 1),
('关于领养流程的说明', '领养流程：浏览宠物信息 → 提交领养申请 → 等待管理员审核 → 审核通过后联系领取。', 1, 1);
