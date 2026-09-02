package com.huace.trace.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 公开查询页返回体（GET /api/hgz/{code}）。
 * 独立 VO 保证 Redis 缓存序列化类型全部在项目包内（多态白名单）。
 */
@Data
public class HgzPublicVO {
    private String code;
    /** 1=生产者 2=收购者 */
    private Integer userType;
    private String productName;
    private String number;
    private String placeOfOrigin;
    private String promiseUser;
    private String contact;
    private LocalDate useTime;
    private String signature;
    private List<Map<String, Object>> promiseList;
    private List<Map<String, Object>> basisList;
    private Integer isShowEnterprise;
    /** 1=有效 0=作废 */
    private Integer status;
    private String qrUrl;
    private String queryUrl;

    private String enterpriseName;
    private String enterpriseIntroduction;
    private String enterpriseImage;
    private String enterpriseSealImage;

    private Long batchId;
    private String batchName;
    private Long goodsId;
}
