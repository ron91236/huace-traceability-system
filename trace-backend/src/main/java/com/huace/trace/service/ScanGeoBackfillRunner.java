package com.huace.trace.service;

import com.huace.trace.mapper.ScanRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanGeoBackfillRunner {

    private final ScanRecordMapper scanRecordMapper;
    private final IpRegionService ipRegionService;

    /** 归属地写入上线前的历史扫码只存了 IP，启动时用离线库补一次省市；补完该查询自然返回空 */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillMissingRegions() {
        List<Map<String, Object>> rows = scanRecordMapper.selectMissingRegion(500);
        int patched = 0;
        for (Map<String, Object> row : rows) {
            String[] region = ipRegionService.resolve((String) row.get("ip"));
            if (region == null) continue;
            scanRecordMapper.updateRegion(((Number) row.get("id")).longValue(), region[0], region[1]);
            patched++;
        }
        if (patched > 0) log.info("回补历史扫码归属地 {} 条", patched);
    }
}
