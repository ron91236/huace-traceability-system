package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.DlLabelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DlLabelVersionMapper extends BaseMapper<DlLabelVersion> {

    /** 按日期统计新增标签版本数 */
    @Select("SELECT DATE(created_at) AS d, COUNT(*) AS cnt FROM dl_label_version " +
            "WHERE product_id IN (SELECT id FROM dl_product WHERE enterprise_id = #{enterpriseId}) " +
            "AND created_at >= #{startTime} GROUP BY DATE(created_at)")
    List<Map<String, Object>> countByDay(@Param("enterpriseId") Long enterpriseId,
                                         @Param("startTime") String startTime);
}
