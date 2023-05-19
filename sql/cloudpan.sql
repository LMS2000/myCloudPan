DROP TABLE IF EXISTS `User`;
CREATE TABLE `user`
(
    `user_id`     int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `username`    varchar(50) DEFAULT NULL COMMENT '用户名',
    `password`    varchar(50) DEFAULT NULL COMMENT '密码',
    `email`       varchar(50) DEFAULT NULL COMMENT '邮箱',
    `is_enable`   tinyint(1) DEFAULT '0' COMMENT '是否可用',
    `use_quota`   bigint(11) DEFAULT NULL COMMENT '使用情况',
    `quota`       bigint(11) DEFAULT NULL COMMENT '总容量',
    `avatar`      varchar(50) DEFAULT NULL COMMENT '头像',
    `user_role`   varchar(50) DEFAULT NULL COMMENT '用户角色',
    `create_time` datetime    DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime    DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8 COMMENT='用户表';

DROP TABLE IF EXISTS `Folder`;
CREATE TABLE `folder`
(
    `folder_id`     int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `folder_name`   varchar(50) NOT NULL COMMENT '文件夹名称',
    `parent_folder` int(11) DEFAULT NULL,
    `size`          bigint(20) DEFAULT '0',
    `user_id`       int(11) NOT NULL COMMENT '所属用户',
    `create_time`   datetime DEFAULT NULL COMMENT '创建时间',
    `update_time`   datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`folder_id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8 COMMENT='文件夹表';


DROP TABLE IF EXISTS `File`;
CREATE TABLE `file`
(
    `file_id`     int(11) NOT NULL AUTO_INCREMENT COMMENT 'id',
    `file_name`   varchar(50)                                             DEFAULT NULL COMMENT '文件名',
    `file_url`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '鏂囦欢璺緞',
    `size`        bigint(11) DEFAULT NULL COMMENT '文件大小',
    `user_id`     int(11) DEFAULT NULL COMMENT '鎵€灞炵敤鎴?',
    `folder_id`   int(11) DEFAULT NULL COMMENT '鎵€灞炴枃浠跺す',
    `file_type`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL,
    `create_time` datetime                                                DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime                                                DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`file_id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8 COMMENT='文件表';

