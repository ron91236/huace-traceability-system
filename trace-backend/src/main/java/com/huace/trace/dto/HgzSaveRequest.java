package com.huace.trace.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 合格证开证/编辑请求体。
 * 与批次/商品绑定后，产品名称、产地、主体、联系方式等服务端自动带出，
 * 前端传值优先。
 */
@Data
public class HgzSaveRequest {
    private Long batchId;
    private Long goodsId;
    /** 1=生产者 2=收购者 */
    private Integer userType;
    private String productName;
    private String number;
    private String placeOfOrigin;
    private String promiseUser;
    private String contact;
    /** yyyy-MM-dd */
    private String useTime;
    private String signature;
    private List<Map<String, Object>> promiseList;
    private List<Map<String, Object>> basisList;
    private Integer isShowEnterprise;
}
