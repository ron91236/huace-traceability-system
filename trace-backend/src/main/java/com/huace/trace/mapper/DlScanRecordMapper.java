package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.DlScanRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DlScanRecordMapper extends BaseMapper<DlScanRecord> {

    /** 按日期统计扫码次数（enterpriseId 为空时统计全部企业） */
    @Select("<script>SELECT DATE(scan_time) AS d, COUNT(*) AS cnt FROM dl_scan_record " +
            "WHERE scan_time &gt;= #{startTime} " +
            "<if test='enterpriseId != null'>AND product_id IN (SELECT id FROM dl_product WHERE enterprise_id = #{enterpriseId}) </if>" +
            "GROUP BY DATE(scan_time) ORDER BY d</script>")
    List<Map<String, Object>> countByDay(@Param("enterpriseId") Long enterpriseId,
                                         @Param("startTime") String startTime);

    /** 按商品统计扫码次数 */
    @Select("<script>SELECT sr.product_id AS productId, p.food_name AS foodName, p.barcode AS barcode, " +
            "p.enterprise_id AS enterpriseId, e.name AS enterpriseName, " +
            "COUNT(*) AS scanCount FROM dl_scan_record sr " +
            "JOIN dl_product p ON p.id = sr.product_id " +
            "LEFT JOIN enterprise e ON e.id = p.enterprise_id " +
            "<where><if test='enterpriseId != null'>p.enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY sr.product_id, p.food_name, p.barcode, p.enterprise_id, e.name ORDER BY scanCount DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> topByProduct(@Param("enterpriseId") Long enterpriseId,
                                           @Param("limit") int limit);

    /** 按版本聚合扫码次数 */
    @Select("<script>SELECT sr.version_id AS versionId, v.version_no AS versionNo, p.food_name AS foodName, " +
            "p.barcode AS barcode, p.enterprise_id AS enterpriseId, e.name AS enterpriseName, " +
            "COUNT(*) AS scanCount FROM dl_scan_record sr " +
            "JOIN dl_label_version v ON v.id = sr.version_id " +
            "JOIN dl_product p ON p.id = sr.product_id " +
            "LEFT JOIN enterprise e ON e.id = p.enterprise_id " +
            "<where><if test='enterpriseId != null'>p.enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY sr.version_id, v.version_no, p.food_name, p.barcode, p.enterprise_id, e.name ORDER BY scanCount DESC</script>")
    List<Map<String, Object>> groupByVersion(@Param("enterpriseId") Long enterpriseId);

    /** 按省份统计扫码分布 */
    @Select("<script>SELECT COALESCE(location_province, '未知') AS province, COUNT(*) AS cnt " +
            "FROM dl_scan_record " +
            "<where><if test='enterpriseId != null'>product_id IN (SELECT id FROM dl_product WHERE enterprise_id = #{enterpriseId})</if></where> " +
            "GROUP BY location_province ORDER BY cnt DESC</script>")
    List<Map<String, Object>> countByProvince(@Param("enterpriseId") Long enterpriseId);
}
