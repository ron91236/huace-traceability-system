package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.dto.LabelInventoryVO;
import com.huace.trace.entity.CodePackage;
import com.huace.trace.entity.LabelInventoryLog;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.mapper.CodePackageMapper;
import com.huace.trace.mapper.LabelInventoryLogMapper;
import com.huace.trace.mapper.LabelSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabelInventoryService {

    private final LabelInventoryLogMapper inventoryLogMapper;
    private final CodePackageMapper codePackageMapper;
    private final LabelSpecMapper labelSpecMapper;

    public List<LabelInventoryVO> getPackageInventory() {
        return inventoryLogMapper.selectPackageInventory();
    }

    public List<LabelInventoryVO> getSpecInventory() {
        return inventoryLogMapper.selectSpecInventory();
    }

    public LabelInventoryVO getPackageInventoryById(Long packageId) {
        return inventoryLogMapper.selectPackageInventoryById(packageId);
    }

    public PageResult<LabelInventoryLog> getLogList(int page, int size, Long packageId, String changeType) {
        LambdaQueryWrapper<LabelInventoryLog> w = new LambdaQueryWrapper<>();
        if (packageId != null) {
            w.eq(LabelInventoryLog::getCodePackageId, packageId);
        }
        if (changeType != null && !changeType.isEmpty()) {
            w.eq(LabelInventoryLog::getChangeType, changeType);
        }
        w.orderByDesc(LabelInventoryLog::getId);
        Page<LabelInventoryLog> r = inventoryLogMapper.selectPage(new Page<>(page, size), w);
        List<LabelInventoryLog> records = r.getRecords();
        if (!records.isEmpty()) {
            List<Long> packageIds = records.stream()
                    .map(LabelInventoryLog::getCodePackageId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            List<Long> specIds = records.stream()
                    .map(LabelInventoryLog::getLabelSpecId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, CodePackage> packageMap = packageIds.isEmpty() ? java.util.Collections.emptyMap()
                    : codePackageMapper.selectBatchIds(packageIds).stream()
                            .collect(Collectors.toMap(CodePackage::getId, cp -> cp));
            Map<Long, LabelSpec> specMap = specIds.isEmpty() ? java.util.Collections.emptyMap()
                    : labelSpecMapper.selectBatchIds(specIds).stream()
                            .collect(Collectors.toMap(LabelSpec::getId, ls -> ls));

            records.forEach(log -> {
                CodePackage cp = packageMap.get(log.getCodePackageId());
                if (cp != null) log.setPackageNo(cp.getPackageNo());
                LabelSpec ls = specMap.get(log.getLabelSpecId());
                if (ls != null) log.setLabelSpecName(ls.getSpecName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    @Transactional
    public void recordInbound(Long codePackageId, Long labelSpecId, Integer quantity, String operatorName, String remark) {
        LabelInventoryLog log = new LabelInventoryLog();
        log.setCodePackageId(codePackageId);
        log.setLabelSpecId(labelSpecId);
        log.setChangeType("IN");
        log.setQuantity(quantity);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    @Transactional
    public void recordVoid(Long codePackageId, Long labelSpecId, Integer quantity,
                          String serialStart, String serialEnd, Long voidedRangeId,
                          String operatorName, String remark) {
        LabelInventoryLog log = new LabelInventoryLog();
        log.setCodePackageId(codePackageId);
        log.setLabelSpecId(labelSpecId);
        log.setChangeType("VOID");
        log.setQuantity(quantity);
        log.setSerialStart(serialStart);
        log.setSerialEnd(serialEnd);
        log.setVoidedRangeId(voidedRangeId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }

    @Transactional
    public void recordShipment(Long codePackageId, Long labelSpecId, Integer quantity,
                              Long orderId, Long orderCodeId,
                              String operatorName, String remark) {
        LabelInventoryLog log = new LabelInventoryLog();
        log.setCodePackageId(codePackageId);
        log.setLabelSpecId(labelSpecId);
        log.setChangeType("SHIP");
        log.setQuantity(quantity);
        log.setOrderId(orderId);
        log.setOrderCodeId(orderCodeId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        inventoryLogMapper.insert(log);
    }
}
