package com.huace.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huace.trace.dto.LabelInventoryVO;
import com.huace.trace.entity.LabelInventoryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LabelInventoryLogMapper extends BaseMapper<LabelInventoryLog> {

    @Select("SELECT " +
            "l.label_spec_id as labelSpecId, " +
            "ls.spec_name as labelSpecName, " +
            "l.code_package_id as codePackageId, " +
            "cp.package_no as packageNo, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) as totalIn, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) as totalVoid, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as totalShip, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as currentStock " +
            "FROM label_inventory_log l " +
            "LEFT JOIN label_spec ls ON l.label_spec_id = ls.id " +
            "LEFT JOIN code_package cp ON l.code_package_id = cp.id " +
            "WHERE l.code_package_id IS NOT NULL " +
            "GROUP BY l.code_package_id, l.label_spec_id, ls.spec_name, cp.package_no " +
            "ORDER BY l.code_package_id DESC")
    List<LabelInventoryVO> selectPackageInventory();

    @Select("SELECT " +
            "l.label_spec_id as labelSpecId, " +
            "ls.spec_name as labelSpecName, " +
            "NULL as codePackageId, " +
            "NULL as packageNo, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) as totalIn, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) as totalVoid, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as totalShip, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as currentStock " +
            "FROM label_inventory_log l " +
            "LEFT JOIN label_spec ls ON l.label_spec_id = ls.id " +
            "WHERE l.label_spec_id IS NOT NULL " +
            "GROUP BY l.label_spec_id, ls.spec_name " +
            "ORDER BY l.label_spec_id DESC")
    List<LabelInventoryVO> selectSpecInventory();

    @Select("SELECT " +
            "l.label_spec_id as labelSpecId, " +
            "ls.spec_name as labelSpecName, " +
            "l.code_package_id as codePackageId, " +
            "cp.package_no as packageNo, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) as totalIn, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) as totalVoid, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as totalShip, " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'IN' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'VOID' THEN l.quantity ELSE 0 END), 0) - " +
            "COALESCE(SUM(CASE WHEN l.change_type = 'SHIP' THEN l.quantity ELSE 0 END), 0) as currentStock " +
            "FROM label_inventory_log l " +
            "LEFT JOIN label_spec ls ON l.label_spec_id = ls.id " +
            "LEFT JOIN code_package cp ON l.code_package_id = cp.id " +
            "WHERE l.code_package_id = #{packageId} " +
            "GROUP BY l.code_package_id, l.label_spec_id, ls.spec_name, cp.package_no")
    LabelInventoryVO selectPackageInventoryById(@Param("packageId") Long packageId);
}
