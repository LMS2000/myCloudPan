DROP TABLE IF EXISTS `User`;
CREATE TABLE User
(
    user_id     INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    username    VARCHAR(50) COMMENT '用户名',
    password    VARCHAR(50) COMMENT '密码',
    email       VARCHAR(50) COMMENT '邮箱',
    is_enable   tinyint(1) DEFAULT 0 COMMENT '是否可用',
    use_quota   bigint(11) COMMENT '使用情况',
    quota       bigint(11) COMMENT '总容量',
    create_time datetime COMMENT '创建时间',
    update_time datetime COMMENT '修改时间'
)ENGINE = InnoDB  COMMENT = '用户表';
DROP TABLE IF EXISTS `Folder`;
CREATE TABLE Folder
(
    folder_id     INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    folder_name   VARCHAR(50) NOT NULL COMMENT '文件夹名称',
    parent_folder int(11) NOT NULL COMMENT '父级文件夹id',
    user_id       int(11) NOT NULL COMMENT '所属用户',
    size          bigint(20) DEFAULT COMMENT '文件夹大小',
    create_time   datetime COMMENT '创建时间',
    update_time   datetime COMMENT '修改时间'
) ENGINE = InnoDB  COMMENT = '文件夹表';
DROP TABLE IF EXISTS `File`;
CREATE TABLE File
(
    file_id     INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT 'id',
    file_name   VARCHAR(50) COMMENT '文件名',
    file_type   VARCHAR(50) COMMENT '文件类型',
    file_path   VARCHAR(255) COMMENT '文件路径',
    file_url    VARCHAR(255) COMMENT '文件url',
    size        BIGINT(11) COMMENT '文件大小',
    create_time datetime COMMENT '创建时间',
    update_time datetime COMMENT '修改时间',
    user_id     INT(11) COMMENT '所属用户',
    folder_id   INT(11) COMMENT '所属文件夹'
) ENGINE = InnoDB  COMMENT = '文件表';