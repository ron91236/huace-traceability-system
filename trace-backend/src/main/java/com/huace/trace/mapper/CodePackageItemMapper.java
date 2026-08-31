package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.entity.CodePackageItem;
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
}
