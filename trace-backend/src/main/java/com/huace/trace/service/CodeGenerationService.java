package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.CodePackage;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.entity.mongo.CodePackageItemMongo;
import com.huace.trace.mapper.CodePackageItemMapper;
import com.huace.trace.mapper.CodePackageMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenerationService {

    private final CodePackageMapper codePackageMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final CodePackageItemService codePackageItemService;
    private final MongoCodeItemService mongoCodeItemService;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MYSQL_BATCH_SIZE = 1000;
    private static final int MONGO_BATCH_SIZE = 5000;

    /**
     * 生成码包
     * 参数: productCode, yearCode, serialDigits, serialStart, serialEnd,
     *       startQuantity, codeType, urlPrefix, remark
     */
    @Transactional
    public CodePackage generate(Map<String, Object> params) {
        String productCode = str(params.get("productCode"));
        String yearCode = str(params.get("yearCode"));
        int serialDigits = intVal(params.get("serialDigits"), 8);
        long serialStart = longVal(params.get("serialStart"));
        long serialEnd = longVal(params.get("serialEnd"));
        long startQuantity = longVal(params.get("startQuantity"));
        String codeType = str(params.get("codeType"));
        String urlPrefix = str(params.get("urlPrefix"));
        String remark = str(params.get("remark"));
        int antiFakeDigits = intVal(params.get("antiFakeDigits"), 10);
        String verifyMode = str(params.get("verifyMode")); // INPUT or DIRECT

        if (antiFakeDigits < 6 || antiFakeDigits > 20) {
            throw new BusinessException("防伪码位数必须在6-20之间");
        }
        if (serialStart <= 0 || serialEnd <= 0 || serialEnd < serialStart) {
            throw new BusinessException("流水号范围不正确");
        }

        int totalCount = (int) (serialEnd - serialStart + 1);
        if (totalCount > 2000000) {
            throw new BusinessException("单次生成数量不能超过200万");
        }

        if (urlPrefix == null || urlPrefix.isEmpty()) {
            urlPrefix = "http://trace.cti-pit.com/trace/";
        }
        if (codeType == null || codeType.isEmpty()) {
            codeType = "SERIAL_URL_ANTI";
        }

        // 创建码包记录
        String packageNo = "GEN" + System.currentTimeMillis();
        CodePackage cp = new CodePackage();
        cp.setPackageNo(packageNo);
        cp.setTotalCount(totalCount);
        cp.setProductCode(productCode);
        cp.setYearCode(yearCode != null ? yearCode : "");
        cp.setSerialDigits(serialDigits);
        cp.setSerialStart(serialStart);
        cp.setSerialEnd(serialEnd);
        cp.setStartQuantity(startQuantity);
        cp.setCodeType(codeType);
        cp.setUrlPrefix(urlPrefix);
        cp.setAntiFakeDigits(antiFakeDigits);
        cp.setVerifyMode(verifyMode != null ? verifyMode : "INPUT");
        cp.setRemark(remark);
        cp.setSourceType("GENERATE");
        cp.setStatus("UNBOUND");
        cp.setImportTime(LocalDateTime.now());
        codePackageMapper.insert(cp);

        // 生成码包明细
        List<CodePackageItem> itemList = new ArrayList<>(totalCount);
        long quantity = startQuantity;
        for (long serial = serialStart; serial <= serialEnd; serial++) {
            String serialNo = String.format("%0" + serialDigits + "d", serial);
            String antiFakeCode = generateAntiFakeCode(antiFakeDigits);
            String url = urlPrefix + serialNo;
            if ("DIRECT".equals(verifyMode)) {
                url += "&direct=1";
            }

            CodePackageItem item = new CodePackageItem();
            item.setPackageId(cp.getId());
            item.setSerialNo(serialNo);
            item.setAntiFakeCode(antiFakeCode);
            item.setUrl(url);
            item.setBindStatus("UNBOUND");
            itemList.add(item);

            quantity++;
        }

        // 批量写入 MySQL（避免逐条 insert 性能瓶颈）
        codePackageItemService.saveBatch(itemList, MYSQL_BATCH_SIZE);

        // 分批写入 MongoDB（外部存储，承载亿级码包）
        List<CodePackageItemMongo> mongoItems = itemList.stream()
                .map(MongoCodeItemService::fromMyBatis)
                .collect(Collectors.toList());
        mongoCodeItemService.saveAll(mongoItems, MONGO_BATCH_SIZE);

        log.info("码包生成完成: packageNo={}, totalCount={}", packageNo, totalCount);
        return cp;
    }

    /**
     * 导出码包为 CSV
     * 格式: 流水号, URL
     */
    public void exportCsv(Long packageId, HttpServletResponse response) throws Exception {
        CodePackage cp = codePackageMapper.selectById(packageId);
        if (cp == null) throw new BusinessException("码包不存在");

        String filename = "code_package_" + cp.getPackageNo() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff'); // BOM for Excel
            writer.println("流水号,防伪码,溯源网址");

            var items = codePackageItemMapper.selectList(
                    new LambdaQueryWrapper<CodePackageItem>()
                            .eq(CodePackageItem::getPackageId, packageId)
                            .orderByAsc(CodePackageItem::getId));

            for (CodePackageItem item : items) {
                writer.println(item.getSerialNo() + "," +
                        (item.getAntiFakeCode() != null ? item.getAntiFakeCode() : "") + "," +
                        (item.getUrl() != null ? item.getUrl() : ""));
            }
        }
    }

    /**
     * 生成防伪码 (固定位数数字)
     */
    private String generateAntiFakeCode(int digits) {
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String str(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private int intVal(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return def; }
    }

    private long longVal(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return 0; }
    }
}
