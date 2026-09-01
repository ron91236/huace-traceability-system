package com.huace.trace.service;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class IpRegionService {

    private volatile Searcher searcher;

    /** 解析 IP 归属地，返回 [省, 市]，解析失败返回 null。 */
    public String[] resolve(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            Searcher s = ensureSearcher();
            if (s == null) return null;
            String region = s.search(ip.trim());
            if (region == null) return null;
            String[] parts = region.split("\\|", -1);
            if (parts.length < 4) return null;
            String province = clean(parts[2]);
            String city = clean(parts[3]);
            // ip2region 对私有地址返回"内网IP"，不视为有效归属地
            if ("内网IP".equals(city)) city = "";
            if (province.isEmpty() && city.isEmpty()) return null;
            return new String[]{province, city};
        } catch (Exception e) {
            return null;
        }
    }

    private Searcher ensureSearcher() {
        Searcher s = searcher;
        if (s != null) return s;
        synchronized (this) {
            s = searcher;
            if (s != null) return s;
            try (InputStream in = new ClassPathResource("ip2region/ip2region.xdb").getInputStream()) {
                searcher = Searcher.newWithBuffer(in.readAllBytes());
            } catch (Exception e) {
                log.warn("ip2region 数据加载失败，扫码记录将不写入省市区: {}", e.getMessage());
            }
            return searcher;
        }
    }

    private String clean(String v) {
        return v == null || "0".equals(v) ? "" : v.trim();
    }
}
