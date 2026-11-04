package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.ScanRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScanRecordMapper extends BaseMapper<ScanRecord> {

    /** 真实消费者扫码：排除本机回环与缺失 IP 的自扫、监控记录（script 内不能用裸 <>，故用 !=） */
    String REAL_SCAN = "ip IS NOT NULL AND ip != '' AND ip NOT LIKE '127.%' " +
            "AND ip NOT IN ('::1', '0:0:0:0:0:0:0:1')";

    @Select("<script>" +
            "SELECT IFNULL(NULLIF(province, ''), '未知') AS province, COUNT(*) AS count FROM scan_record " +
            "<where> " + REAL_SCAN +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY province ORDER BY count DESC" +
            "</script>")
    List<Map<String, Object>> countByProvince(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT IFNULL(NULLIF(city, ''), '未知') AS city, COUNT(*) AS count FROM scan_record " +
            "<where> " + REAL_SCAN +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if></where> " +
            "GROUP BY city ORDER BY count DESC LIMIT 10" +
            "</script>")
    List<Map<String, Object>> countByCity(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT DATE(created_at) AS date, COUNT(*) AS count FROM scan_record " +
            "<where> created_at >= DATE_SUB(NOW(), INTERVAL 3 MONTH) AND " + REAL_SCAN +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if>" +
            "</where> GROUP BY DATE(created_at) ORDER BY date" +
            "</script>")
    List<Map<String, Object>> scanTrend(@Param("enterpriseId") Long enterpriseId);

    @Select("<script>" +
            "SELECT enterprise_id, COUNT(*) AS count FROM scan_record " +
            "<where> enterprise_id IS NOT NULL AND " + REAL_SCAN + "</where> " +
            "GROUP BY enterprise_id ORDER BY count DESC LIMIT 10" +
            "</script>")
    List<Map<String, Object>> countByEnterprise();

    /** 已绑定码中被真实消费者扫过的去重码数 */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT s.serial_no) AS count FROM scan_record s " +
            "JOIN code_package_item i ON i.serial_no = s.serial_no AND i.bind_status = 'BOUND' " +
            "<where> s.ip IS NOT NULL AND s.ip != '' AND s.ip NOT LIKE '127.%' " +
            "AND s.ip NOT IN ('::1', '0:0:0:0:0:0:0:1')" +
            "<if test='enterpriseId != null'> AND i.enterprise_id = #{enterpriseId}</if>" +
            "</where>" +
            "</script>")
    Long countScannedBoundCodes(@Param("enterpriseId") Long enterpriseId);

    /** 归属地写入上线前留下的历史扫码：有公网 IP 但省市为空 */
    @Select("SELECT id, ip FROM scan_record WHERE province IS NULL AND " + REAL_SCAN + " LIMIT #{limit}")
    List<Map<String, Object>> selectMissingRegion(@Param("limit") int limit);

    @Update("UPDATE scan_record SET province = #{province}, city = #{city} WHERE id = #{id}")
    int updateRegion(@Param("id") Long id, @Param("province") String province, @Param("city") String city);
}
