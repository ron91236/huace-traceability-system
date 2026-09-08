package com.huace.trace.dto;

import lombok.Data;

@Data
public class LabelInventoryVO {
    private Long labelSpecId;
    private String labelSpecName;
    private Long codePackageId;
    private String packageNo;
    private Integer totalIn;
    private Integer totalVoid;
    private Integer totalShip;
    private Integer currentStock;
}
