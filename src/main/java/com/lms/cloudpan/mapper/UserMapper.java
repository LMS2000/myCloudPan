package com.lms.cloudpan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lms.cloudpan.entity.dao.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;
import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {


}
