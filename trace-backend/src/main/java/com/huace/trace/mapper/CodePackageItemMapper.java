package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.CodePackageItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface CodePackageItemMapper extends BaseMapper<CodePackageItem> {

    @Select("<script>" +
            "SELECT package_id, bind_status, COUNT(*) as cnt FROM code_package_item " +
            "WHERE package_id IN " +
            "<foreach collection='packageIds' item='pid' open='(' separator=',' close=')'>#{pid}</foreach> " +
            "GROUP BY package_id, bind_status" +
            "</script>")
    List<Map<String, Object>> countGroupByPackageAndStatus(@Param("packageIds") Collection<Long> packageIds);

    /** 分批删除，避免单条 DELETE 百万行造成长事务与锁表 */
    @Delete("DELETE FROM code_package_item WHERE package_id = #{packageId} LIMIT #{limit}")
    int deleteBatchByPackageId(@Param("packageId") Long packageId, @Param("limit") int limit);

    /** 标签发放趋势：按绑定（发放到企业/商品）时间按月统计 */
    @Select("<script>" +
            "SELECT DATE_FORMAT(bind_time, '%Y-%m') AS month, COUNT(*) AS count FROM code_package_item " +
            "<where> bind_time >= DATE_SUB(NOW(), INTERVAL 6 MONTH)" +
            "<if test='enterpriseId != null'> AND enterprise_id = #{enterpriseId}</if>" +
            "</where> GROUP BY month ORDER BY month" +
            "</script>")
    List<Map<String, Object>> monthlyBindings(@Param("enterpriseId") Long enterpriseId);
}
