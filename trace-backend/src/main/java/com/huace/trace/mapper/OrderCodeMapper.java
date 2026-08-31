package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huace.trace.entity.OrderCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderCodeMapper extends BaseMapper<OrderCode> {

    /**
     * 发放管理分页：过滤条件下沉 SQL（join t_order / enterprise_cert），
     * 保证 count 与分页基于同一结果集，total 正确
     */
    @Select("<script>" +
            "SELECT oc.* FROM order_code oc " +
            "LEFT JOIN t_order o ON o.id = oc.order_id " +
            "LEFT JOIN enterprise_cert c ON c.id = o.cert_id " +
            "<where>" +
            "<if test='enterpriseId != null'> AND o.enterprise_id = #{enterpriseId}</if>" +
            "<if test='orderNo != null and orderNo != \"\"'> AND o.order_no LIKE CONCAT('%', #{orderNo}, '%')</if>" +
            "<if test='certNo != null and certNo != \"\"'> AND c.cert_no LIKE CONCAT('%', #{certNo}, '%')</if>" +
            "</where> " +
            "ORDER BY oc.id DESC" +
            "</script>")
    IPage<OrderCode> selectDistributionPage(IPage<OrderCode> page,
                                            @Param("enterpriseId") Long enterpriseId,
                                            @Param("orderNo") String orderNo,
                                            @Param("certNo") String certNo);
}
