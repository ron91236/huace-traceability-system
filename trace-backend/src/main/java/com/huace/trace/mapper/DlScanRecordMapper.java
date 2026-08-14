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

    /** 按日期统计扫码次数 */
    @Select("SELECT DATE(scan_time) AS d, COUNT(*) AS cnt FROM dl_scan_record " +
            "WHERE product_id IN (SELECT id FROM dl_product WHERE enterprise_id = #{enterpriseId}) " +
            "AND scan_time >= #{startTime} GROUP BY DATE(scan_time) ORDER BY d")
    List<Map<String, Object>> countByDay(@Param("enterpriseId") Long enterpriseId,
                                         @Param("startTime") String startTime);

    /** 按商品统计扫码次数 */
    @Select("SELECT sr.product_id AS productId, p.food_name AS foodName, p.barcode AS barcode, " +
            "COUNT(*) AS scanCount FROM dl_scan_record sr " +
            "JOIN dl_product p ON p.id = sr.product_id " +
            "WHERE p.enterprise_id = #{enterpriseId} " +
            "GROUP BY sr.product_id, p.food_name, p.barcode ORDER BY scanCount DESC LIMIT #{limit}")
    List<Map<String, Object>> topByProduct(@Param("enterpriseId") Long enterpriseId,
                                           @Param("limit") int limit);

    /** 按版本聚合扫码次数 */
    @Select("SELECT sr.version_id AS versionId, v.version_no AS versionNo, p.food_name AS foodName, " +
            "p.barcode AS barcode, COUNT(*) AS scanCount FROM dl_scan_record sr " +
            "JOIN dl_label_version v ON v.id = sr.version_id " +
            "JOIN dl_product p ON p.id = sr.product_id " +
            "WHERE p.enterprise_id = #{enterpriseId} " +
            "GROUP BY sr.version_id, v.version_no, p.food_name, p.barcode ORDER BY scanCount DESC")
    List<Map<String, Object>> groupByVersion(@Param("enterpriseId") Long enterpriseId);

    /** 按省份统计扫码分布 */
    @Select("SELECT COALESCE(location_province, '未知') AS province, COUNT(*) AS cnt " +
            "FROM dl_scan_record WHERE product_id IN (SELECT id FROM dl_product WHERE enterprise_id = #{enterpriseId}) " +
            "GROUP BY location_province ORDER BY cnt DESC")
    List<Map<String, Object>> countByProvince(@Param("enterpriseId") Long enterpriseId);
}
