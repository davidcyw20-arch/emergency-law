package com.yunxian.emergencylaw.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunxian.emergencylaw.entity.StudyProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudyProgressMapper extends BaseMapper<StudyProgress> {
    int upsert(@Param("p") StudyProgress p);
}
