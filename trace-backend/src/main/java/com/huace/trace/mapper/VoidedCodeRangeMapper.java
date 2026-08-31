package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.VoidedCodeRange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VoidedCodeRangeMapper extends BaseMapper<VoidedCodeRange> {

    @Select("SELECT CAST(COALESCE(SUM(LEAST(CAST(serial_end AS UNSIGNED), #{end}) - GREATEST(CAST(serial_start AS UNSIGNED), #{start}) + 1), 0) AS UNSIGNED) " +
            "FROM voided_code_range " +
            "WHERE serial_start REGEXP '^[0-9]+$' AND serial_end REGEXP '^[0-9]+$' " +
            "AND CAST(serial_start AS UNSIGNED) <= CAST(serial_end AS UNSIGNED) " +
            "AND CAST(serial_start AS UNSIGNED) <= #{end} " +
            "AND CAST(serial_end AS UNSIGNED) >= #{start}")
    Long sumOverlappingCount(@Param("start") long start, @Param("end") long end);
}
