package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.ScanRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScanRecordMapper extends BaseMapper<ScanRecord> {

    @Select("<script>" +
            "SELECT province, COUNT(*) as count FROM scan_record " +
            "<where><if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY province ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> countByProvince(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT city, COUNT(*) as count FROM scan_record " +
            "<where><if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY city ORDER BY count DESC LIMIT 10" +
            "</script>")
    List<Map<String, Object>> countByCity(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT DATE(created_at) as date, COUNT(*) as count FROM scan_record " +
            "<where> created_at >= DATE_SUB(NOW(), INTERVAL 3 MONTH)" +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if>" +
            "</where> GROUP BY DATE(created_at) ORDER BY date" +
            "</script>")
    List<Map<String, Object>> scanTrend(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COUNT(*) as count FROM scan_record " +
            "<where> created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)" +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if>" +
            "</where> GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month" +
            "</script>")
    List<Map<String, Object>> monthlyScans(@Param("enterpriseId") Long enterpriseId);
}
