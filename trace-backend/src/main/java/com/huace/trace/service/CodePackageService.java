package com.huace.trace.service;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.entity.mongo.CodePackageItemMongo;
import com.huace.trace.mapper.*;
import com.huace.trace.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodePackageService {

    private final CodePackageMapper codePackageMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final FileUploadUtil fileUploadUtil;
    private final MongoCodeItemService mongoCodeItemService;

    public PageResult<CodePackage> list(int page, int size, String keyword) {
        LambdaQueryWrapper<CodePackage> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            w.like(CodePackage::getPackageNo, keyword);
        }
        w.orderByDesc(CodePackage::getId);
        Page<CodePackage> r = codePackageMapper.selectPage(new Page<>(page, size), w);
        List<CodePackage> records = r.getRecords();
        if (!records.isEmpty()) {
            List<Long> packageIds = records.stream().map(CodePackage::getId).collect(Collectors.toList());
            List<Long> specIds = records.stream().map(CodePackage::getLabelSpecId)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, LabelSpec> specMap = specIds.isEmpty() ? java.util.Collections.emptyMap()
                    : labelSpecMapper.selectBatchIds(specIds).stream()
                            .collect(Collectors.toMap(LabelSpec::getId, ls -> ls));
            Map<Long, Long> boundMap = codePackageItemMapper.countGroupByPackageAndStatus(packageIds).stream()
                    .filter(m -> "BOUND".equals(m.get("bind_status")))
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("package_id")).longValue(),
                            m -> ((Number) m.get("cnt")).longValue()));
            records.forEach(cp -> {
                LabelSpec ls = specMap.get(cp.getLabelSpecId());
                if (ls != null) cp.setLabelSpecName(ls.getSpecName());
                cp.setBoundCount(boundMap.getOrDefault(cp.getId(), 0L).intValue());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public Map<String, Object> getDetail(Long id) {
        CodePackage cp = codePackageMapper.selectById(id);
        if (cp == null) throw new BusinessException("码包不存在");

        List<CodePackageItem> items = codePackageItemMapper.selectList(
                new LambdaQueryWrapper<CodePackageItem>()
                        .eq(CodePackageItem::getPackageId, id)
                        .orderByAsc(CodePackageItem::getId));

        return Map.of("package", cp, "items", items);
    }

    @Transactional
    public void importFile(MultipartFile file) throws Exception {
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("csv") && !ext.equalsIgnoreCase("txt"))) {
            throw new BusinessException("仅支持 CSV/TXT 格式文件");
        }

        // 保存文件
        SysFile sysFile = fileUploadUtil.upload(file, null);

        // 创建码包记录
        String packageNo = "PKG" + System.currentTimeMillis();
        CodePackage codePackage = new CodePackage();
        codePackage.setPackageNo(packageNo);
        codePackage.setTotalCount(0);
        codePackage.setSourceFile(sysFile.getFilePath());
        codePackage.setStatus("UNBOUND");
        codePackage.setImportTime(LocalDateTime.now());
        codePackageMapper.insert(codePackage);

        // 解析CSV文件
        List<CodePackageItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // 跳过表头
                }
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    CodePackageItem item = new CodePackageItem();
                    item.setPackageId(codePackage.getId());
                    item.setUrl(parts[0].trim());
                    item.setSerialNo(parts.length > 1 ? parts[1].trim() : "");
                    item.setAntiFakeCode(parts.length > 2 ? parts[2].trim() : "");
                    item.setBindStatus("UNBOUND");
                    items.add(item);
                }
            }
        }

        // 批量插入 MySQL
        for (CodePackageItem item : items) {
            codePackageItemMapper.insert(item);
        }

        // 异步批量写入 MongoDB（亿级码包外部存储，不阻塞导入响应）
        List<CodePackageItemMongo> mongoItems = items.stream()
                .map(MongoCodeItemService::fromMyBatis)
                .collect(Collectors.toList());
        mongoCodeItemService.saveAll(mongoItems);

        // 更新码包总数
        codePackage.setTotalCount(items.size());
        codePackageMapper.updateById(codePackage);
    }

    @Transactional
    public void bind(Long packageId, Map<String, Object> bindData) {
        CodePackage cp = codePackageMapper.selectById(packageId);
        if (cp == null) throw new BusinessException("码包不存在");

        Long enterpriseId = toLong(bindData.get("enterpriseId"));
        Long goodsId = toLong(bindData.get("goodsId"));
        Long certId = toLong(bindData.get("certId"));
        Long batchId = toLong(bindData.get("batchId"));
        String traceTemplate = (String) bindData.get("traceTemplate");
        List<Long> itemIds = (List<Long>) bindData.get("itemIds");

        LambdaQueryWrapper<CodePackageItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CodePackageItem::getPackageId, packageId);
        wrapper.eq(CodePackageItem::getBindStatus, "UNBOUND");
        if (itemIds != null && !itemIds.isEmpty()) {
            wrapper.in(CodePackageItem::getId, itemIds);
        }

        List<CodePackageItem> items = codePackageItemMapper.selectList(wrapper);
        if (items.isEmpty()) throw new BusinessException("没有可绑定的码");

        LocalDateTime now = LocalDateTime.now();
        for (CodePackageItem item : items) {
            item.setEnterpriseId(enterpriseId);
            item.setGoodsId(goodsId);
            item.setCertId(certId);
            item.setBatchId(batchId);
            item.setTraceTemplate(traceTemplate);
            item.setBindStatus("BOUND");
            item.setBindTime(now);
            codePackageItemMapper.updateById(item);
            mongoCodeItemService.save(MongoCodeItemService.fromMyBatis(item));
        }

        // 更新码包状态
        Long unboundCount = codePackageItemMapper.selectCount(
                new LambdaQueryWrapper<CodePackageItem>()
                        .eq(CodePackageItem::getPackageId, packageId)
                        .eq(CodePackageItem::getBindStatus, "UNBOUND"));
        if (unboundCount == 0) {
            cp.setStatus("BOUND");
        } else {
            cp.setStatus("PARTIAL");
        }
        codePackageMapper.updateById(cp);
    }

    public void delete(Long id) {
        CodePackage cp = codePackageMapper.selectById(id);
        if (cp == null) throw new BusinessException("码包不存在");
        if (!"UNBOUND".equals(cp.getStatus())) {
            String statusLabel = "PARTIAL".equals(cp.getStatus()) ? "部分绑定" : "已绑定";
            throw new BusinessException("只能删除未绑定状态的码包，当前状态：" + statusLabel);
        }
        codePackageItemMapper.delete(
                new LambdaQueryWrapper<CodePackageItem>().eq(CodePackageItem::getPackageId, id));
        mongoCodeItemService.deleteByPackageId(id);
        codePackageMapper.deleteById(id);
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return Long.parseLong(obj.toString());
    }
}
