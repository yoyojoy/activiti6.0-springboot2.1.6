package com.shengyecapital.process.mapper;

import com.shengyecapital.process.dto.vo.ProcessDeployedListVo;
import com.shengyecapital.process.dto.vo.RuntimeInstanceListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActivitiMapper {

    @Select("${sql}")
    List<ProcessDeployedListVo> queryDeployedProcessesList(@Param("sql") String sql);
    @Select("${sql}")
    List<RuntimeInstanceListVo> queryRuntimeInstanceInfoList(@Param("sql") String sql);
}
