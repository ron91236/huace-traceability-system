package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CodePackage;
import com.huace.trace.entity.VoidedCodeRange;
import com.huace.trace.mapper.CodePackageMapper;
import com.huace.trace.mapper.VoidedCodeRangeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoidedCodeRangeService {

    private final VoidedCodeRangeMapper voidedCodeRangeMapper;
    private final CodePackageMapper codePackageMapper;

    public PageResult<VoidedCodeRange> list(int page, int size) {
        LambdaQueryWrapper<VoidedCodeRange> w = new LambdaQueryWrapper<>();
        w.orderByDesc(VoidedCodeRange::getId);
        Page<VoidedCodeRange> r = voidedCodeRangeMapper.selectPage(new Page<>(page, size), w);
        List<VoidedCodeRange> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取码包，消除逐行查询
            java.util.Set<Long> pkgIds = records.stream().map(VoidedCodeRange::getCodePackageId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, CodePackage> pkgMap = pkgIds.isEmpty() ? java.util.Collections.emptyMap()
                    : codePackageMapper.selectBatchIds(pkgIds).stream()
                            .collect(java.util.stream.Collectors.toMap(CodePackage::getId, cp -> cp));
            records.forEach(item -> {
                CodePackage cp = pkgMap.get(item.getCodePackageId());
                if (cp != null) item.setPackageNo(cp.getPackageNo());
                if (item.getPackageNo() == null) {
                    item.setPackageNo(item.getSerialDigits() + "位身份码条码库");
                }
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    @Transactional
    public void batchImport(List<VoidedCodeRange> ranges) {
        for (VoidedCodeRange range : ranges) {
            try {
                long start = Long.parseLong(range.getSerialStart());
                long end = Long.parseLong(range.getSerialEnd());
                if (end < start) {
                    throw new BusinessException("结束身份码不能小于开始身份码: " + range.getSerialStart() + " ~ " + range.getSerialEnd());
                }
                range.setCount((int) (end - start + 1));
            } catch (NumberFormatException e) {
                throw new BusinessException("身份码格式不正确，必须为数字");
            }
            voidedCodeRangeMapper.insert(range);
        }
    }

    public void delete(Long id) {
        voidedCodeRangeMapper.deleteById(id);
    }

    /**
     * 计算 [serialStart, serialEnd] 范围内与作废码表重叠的数量
     */
    public int countOverlapping(long serialStart, long serialEnd) {
        List<VoidedCodeRange> allRanges = voidedCodeRangeMapper.selectList(null);
        int totalOverlap = 0;
        for (VoidedCodeRange range : allRanges) {
            try {
                long rStart = Long.parseLong(range.getSerialStart());
                long rEnd = Long.parseLong(range.getSerialEnd());
                long overlapStart = Math.max(rStart, serialStart);
                long overlapEnd = Math.min(rEnd, serialEnd);
                if (overlapStart <= overlapEnd) {
                    totalOverlap += (int)(overlapEnd - overlapStart + 1);
                }
            } catch (NumberFormatException ignored) {
                // 非数字格式跳过
            }
        }
        return totalOverlap;
    }
}
