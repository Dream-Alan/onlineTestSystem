-- ==========================================================
-- 在线考试系统 数据库初始化脚本
-- ==========================================================

CREATE DATABASE IF NOT EXISTS online_test_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE online_test_system;

-- ----------------------------
-- 用户表（教师 + 学生共用）
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role` VARCHAR(20) NOT NULL COMMENT 'student | teacher',
    `username` VARCHAR(50) NOT NULL COMMENT '学号/工号',
    `password` VARCHAR(64) NOT NULL COMMENT 'MD5加密后的密码',
    `name` VARCHAR(50) NOT NULL,
    `gender` VARCHAR(10) DEFAULT NULL COMMENT 'male | female',
    `phone` VARCHAR(20) DEFAULT NULL,
    `college` VARCHAR(100) DEFAULT NULL COMMENT '所在学院',
    `major` VARCHAR(100) DEFAULT NULL COMMENT '专业（学生）',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级（学生）',
    `avatar` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_username_role` (`username`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 课程表
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(50) DEFAULT NULL,
    `semester` VARCHAR(50) DEFAULT NULL,
    `teacher_id` BIGINT NOT NULL COMMENT '任课教师',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 学生选课关联表
-- ----------------------------
DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_student_course` (`student_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 试题表
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` BIGINT NOT NULL,
    `type` VARCHAR(20) NOT NULL COMMENT 'single | multiple | judge | fill | essay',
    `title` VARCHAR(1000) NOT NULL,
    `options` TEXT DEFAULT NULL COMMENT 'JSON数组: [{content}]，仅单选/多选有效',
    `answer` TEXT DEFAULT NULL COMMENT 'JSON: 单选下标/多选下标数组/判断布尔/填空简答字符串',
    `analysis` VARCHAR(2000) DEFAULT NULL,
    `score` INT NOT NULL DEFAULT 5,
    `difficulty` VARCHAR(20) DEFAULT 'easy' COMMENT 'easy | medium | hard',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 考试表
-- ----------------------------
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL,
    `course_id` BIGINT NOT NULL,
    `duration` INT NOT NULL COMMENT '考试时长（分钟）',
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT 'draft | published',
    `allow_retake` TINYINT(1) NOT NULL DEFAULT 0,
    `compose_type` VARCHAR(20) NOT NULL DEFAULT 'manual' COMMENT 'manual | random',
    `total_score` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 考试-试题关联表
-- ----------------------------
DROP TABLE IF EXISTS `exam_question`;
CREATE TABLE `exam_question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `order_num` INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 考试记录表（每次答题一条，支持重考）
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `attempt_no` INT NOT NULL DEFAULT 1,
    `state` VARCHAR(20) NOT NULL DEFAULT 'ongoing' COMMENT 'ongoing | finished',
    `answers_json` TEXT DEFAULT NULL COMMENT '暂存答案 JSON',
    `score` INT DEFAULT NULL,
    `accuracy` DECIMAL(5,2) DEFAULT NULL,
    `start_time` DATETIME NOT NULL,
    `submit_time` DATETIME DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 答题详情表（提交后生成，每道题一条）
-- ----------------------------
DROP TABLE IF EXISTS `exam_answer`;
CREATE TABLE `exam_answer` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `exam_record_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` TEXT DEFAULT NULL COMMENT 'JSON',
    `is_correct` TINYINT(1) DEFAULT NULL,
    `score` INT NOT NULL DEFAULT 0,
    `full_score` INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================================================
-- 初始化测试数据（密码均为 123456，MD5: e10adc3949ba59abbe56e057f20f883e）
-- ==========================================================

INSERT INTO `user` (`id`, `role`, `username`, `password`, `name`, `gender`, `phone`, `college`, `major`, `class_name`) VALUES
(1, 'teacher', 'T001', 'e10adc3949ba59abbe56e057f20f883e', '李老师', 'female', '13900139000', '计算机学院', NULL, NULL),
(2, 'student', '2021001', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'male', '13800138000', '计算机学院', '软件工程', '软件2101'),
(3, 'student', '2021002', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'male', '13800138001', '计算机学院', '软件工程', '软件2101');

INSERT INTO `course` (`id`, `name`, `code`, `semester`, `teacher_id`) VALUES
(1, '数据结构', 'CS101', '2024春季', 1);

INSERT INTO `student_course` (`student_id`, `course_id`) VALUES
(2, 1),
(3, 1);

INSERT INTO `question` (`id`, `course_id`, `type`, `title`, `options`, `answer`, `analysis`, `score`, `difficulty`) VALUES
(1, 1, 'single', '下列哪个是线性数据结构？',
    '[{"content":"栈"},{"content":"队列"},{"content":"二叉树"},{"content":"图"}]',
    '0', '栈和队列都是线性结构', 5, 'easy'),
(2, 1, 'multiple', '下列哪些是常见的排序算法？',
    '[{"content":"冒泡排序"},{"content":"快速排序"},{"content":"深度优先搜索"},{"content":"归并排序"}]',
    '[0,1,3]', '深度优先搜索是图遍历算法，不是排序算法', 10, 'medium'),
(3, 1, 'judge', '队列是先进先出（FIFO）的数据结构。',
    NULL, 'true', '队列遵循先进先出原则', 5, 'easy'),
(4, 1, 'fill', '在二叉树中，度为0的结点称为____。',
    NULL, '叶子结点', '度为0即没有子结点的结点', 5, 'medium'),
(5, 1, 'essay', '简述栈和队列的区别。',
    NULL, '栈是后进先出，队列是先进先出', '考察对基本数据结构特性的理解', 10, 'hard');

INSERT INTO `exam` (`id`, `name`, `course_id`, `duration`, `start_time`, `end_time`, `status`, `allow_retake`, `compose_type`, `total_score`) VALUES
(1, '数据结构期中考试', 1, 90, '2024-01-01 00:00:00', '2030-12-31 23:59:59', 'published', 1, 'manual', 35);

INSERT INTO `exam_question` (`exam_id`, `question_id`, `order_num`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(1, 5, 5);