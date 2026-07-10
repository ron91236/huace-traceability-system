package com.huace.trace.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OrderBarcodeExportDTO {
    @ExcelProperty("订单编号")
    @ColumnWidth(22)
    private String orderNo;

    @ExcelProperty("企业名称")
    @ColumnWidth(20)
    private String enterpriseName;

    @ExcelProperty("产品名称")
    @ColumnWidth(16)
    private String productName;

    @ExcelProperty("产品描述")
    @ColumnWidth(20)
    private String productDescription;

    @ExcelProperty("商品名称")
    @ColumnWidth(14)
    private String goodsName;

    @ExcelProperty("标签规格")
    @ColumnWidth(16)
    private String labelSpecName;

    @ExcelProperty("起始身份码")
    @ColumnWidth(14)
    private String serialStart;

    @ExcelProperty("结束身份码")
    @ColumnWidth(14)
    private String serialEnd;

    @ExcelProperty("数量(枚)")
    @ColumnWidth(10)
    private Integer quantity;

    @ExcelProperty("作废数量")
    @ColumnWidth(10)
    private Integer wasteCount;

    @ExcelProperty("绑定数量")
    @ColumnWidth(10)
    private Integer bindCount;

    @ExcelProperty("生产批次")
    @ColumnWidth(16)
    private String productionBatch;

    @ExcelProperty("生产时间")
    @ColumnWidth(18)
    private String productionTime;
}
