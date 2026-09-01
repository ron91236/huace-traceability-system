package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 防伪码验证核心规则回归测试：
 * 1. 完整防伪码匹配
 * 2. 后4-6位匹配
 * 3. 错误防伪码不递增扫码次数
 * 4. 空输入
 * 5. 流水号不存在
 */
@ExtendWith(MockitoExtension.class)
class TracePageServiceTest {

    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private GoodsMapper goodsMapper;
    @Mock private EnterpriseCertMapper certMapper;
    @Mock private CertTypeMapper certTypeMapper;
    @Mock private BatchMapper batchMapper;
    @Mock private EnterpriseBaseMapper baseMapper;
    @Mock private TraceTemplateMapper templateMapper;
    @Mock private ScanRecordMapper scanRecordMapper;
    @Mock private TestReportMapper testReportMapper;
    @Mock private EnterpriseTemplateDataMapper templateDataMapper;
    @Mock private TestReportService testReportService;
    @Mock private ObjectMapper objectMapper;
    @Mock private MongoCodeItemService mongoCodeItemService;
    @Mock private OrderCodeMapper orderCodeMapper;
    @Mock private IpRegionService ipRegionService;

    @InjectMocks
    private TracePageService service;

    @BeforeEach
    void setUp() {
        lenient().when(mongoCodeItemService.findBySerialNo(anyString())).thenReturn(Optional.empty());
        Enterprise ent = new Enterprise();
        ent.setId(1L);
        ent.setName("河南福鹿家鲜啤酒业有限公司");
        lenient().when(enterpriseMapper.selectById(1L)).thenReturn(ent);
        Goods goods = new Goods();
        goods.setId(2L);
        goods.setName("德式小麦");
        lenient().when(goodsMapper.selectById(2L)).thenReturn(goods);
        EnterpriseCert cert = new EnterpriseCert();
        cert.setId(3L);
        cert.setCertName("全程可追溯产品验证证书");
        lenient().when(certMapper.selectById(3L)).thenReturn(cert);
    }

    private CodePackageItem buildItem(String antiFakeCode) {
        CodePackageItem item = new CodePackageItem();
        item.setId(1L);
        item.setSerialNo("00000626");
        item.setAntiFakeCode(antiFakeCode);
        item.setScanCount(5);
        item.setEnterpriseId(1L);
        item.setGoodsId(2L);
        item.setCertId(3L);
        item.setBindStatus("BOUND");
        return item;
    }

    @Test
    void 完整防伪码匹配_验证通过并递增扫码次数() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "1234567890");

        assertEquals(true, result.get("verified"));
        assertEquals(6, result.get("scanCount"));
        assertEquals("验证通过，该产品为正品", result.get("message"));
        assertEquals("河南福鹿家鲜啤酒业有限公司", result.get("enterpriseName"));
        assertEquals("德式小麦", result.get("productName"));
        assertEquals("全程可追溯产品验证证书", result.get("certName"));
        verify(codePackageItemMapper).updateById(any(CodePackageItem.class));
        verify(mongoCodeItemService).updateScanCount(eq("00000626"), eq(6));
    }

    @Test
    void 后四位防伪码匹配_验证通过() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "7890");

        assertEquals(true, result.get("verified"));
        assertEquals(6, result.get("scanCount"));
    }

    @Test
    void 后六位防伪码匹配_验证通过() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "567890");

        assertEquals(true, result.get("verified"));
    }

    @Test
    void 错误防伪码_验证失败且不递增扫码次数() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "9999");

        assertEquals(false, result.get("verified"));
        assertEquals(5, result.get("scanCount"));
        assertEquals("验证失败，防伪码不匹配，请谨防假冒", result.get("message"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
        verify(mongoCodeItemService, never()).updateScanCount(anyString(), anyInt());
    }

    @Test
    void 三位输入_不匹配() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "890");

        assertEquals(false, result.get("verified"));
    }

    @Test
    void 空输入_验证失败() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "");

        assertEquals(false, result.get("verified"));
        assertEquals(5, result.get("scanCount"));
    }

    @Test
    void 流水号不存在_返回未找到() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        Map<String, Object> result = service.verifyAntiFakeCode("99999999", "123456");

        assertEquals(false, result.get("verified"));
        assertEquals("未找到该产品的溯源信息", result.get("message"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
    }
}
