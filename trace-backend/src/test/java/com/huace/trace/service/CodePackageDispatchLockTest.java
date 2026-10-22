package com.huace.trace.service;

import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.CodePackage;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.mapper.*;
import com.huace.trace.util.FileUploadUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 码包下发锁定范围回归测试：下发只锁"删除码包"，绑定/解绑属于正常业务流转必须继续放行。
 * 1. 已下发码包仍可绑定码明细，且不改变下发状态
 * 2. 已下发码包删除被拒绝，且不触发任何删除动作
 * 3. 未下发码包仍可正常删除
 */
@ExtendWith(MockitoExtension.class)
class CodePackageDispatchLockTest {

    @Mock private CodePackageMapper codePackageMapper;
    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private LabelSpecMapper labelSpecMapper;
    @Mock private FileUploadUtil fileUploadUtil;
    @Mock private MongoCodeItemService mongoCodeItemService;
    @Mock private CodePackageAuditLogMapper auditLogMapper;
    @Mock private LabelInventoryService labelInventoryService;
    @Mock private OrderCodeMapper orderCodeMapper;
    @Mock private VoidedCodeRangeMapper voidedCodeRangeMapper;
    @Mock private LabelInventoryLogMapper labelInventoryLogMapper;

    @InjectMocks
    private CodePackageService service;

    private CodePackage aPackage(boolean dispatched) {
        CodePackage cp = new CodePackage();
        cp.setId(32L);
        cp.setPackageNo("CP-2026-0032");
        cp.setDispatched(dispatched);
        cp.setStatus("UNBOUND");
        return cp;
    }

    @Test
    void 已下发码包仍可绑定码明细() {
        CodePackage cp = aPackage(true);
        when(codePackageMapper.selectById(32L)).thenReturn(cp);
        CodePackageItem item = new CodePackageItem();
        item.setId(1L);
        item.setPackageId(32L);
        item.setSerialNo("00000002");
        when(codePackageItemMapper.selectList(any())).thenReturn(List.of(item));
        when(codePackageItemMapper.selectCount(any())).thenReturn(0L);

        service.bind(32L, Map.of("enterpriseId", 23L, "goodsId", 1L, "batchId", 2L));

        assertEquals("BOUND", item.getBindStatus());
        verify(codePackageItemMapper).updateById(item);
        verify(codePackageMapper).updateById(cp);
        assertEquals("BOUND", cp.getStatus());
        assertTrue(cp.getDispatched(), "绑定不应改变下发状态");
    }

    @Test
    void 已下发码包禁止删除() {
        when(codePackageMapper.selectById(32L)).thenReturn(aPackage(true));

        BusinessException e = assertThrows(BusinessException.class, () -> service.delete(32L));

        assertTrue(e.getMessage().contains("禁止删除"), e.getMessage());
        verify(codePackageMapper, never()).deleteById(any(Long.class));
        verify(codePackageItemMapper, never()).deleteBatchByPackageId(anyLong(), anyInt());
        verify(mongoCodeItemService, never()).deleteByPackageId(anyLong());
    }

    @Test
    void 未下发码包可以删除() {
        when(codePackageMapper.selectById(32L)).thenReturn(aPackage(false));
        when(codePackageItemMapper.deleteBatchByPackageId(anyLong(), anyInt())).thenReturn(0);

        service.delete(32L);

        verify(codePackageMapper).deleteById(any(Long.class));
        verify(orderCodeMapper).delete(any());
        verify(voidedCodeRangeMapper).delete(any());
        verify(labelInventoryLogMapper).delete(any());
    }
}
