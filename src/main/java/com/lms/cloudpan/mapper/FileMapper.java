package com.lms.cloudpan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lms.cloudpan.entity.dao.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FileMapper extends BaseMapper<File> {


}
