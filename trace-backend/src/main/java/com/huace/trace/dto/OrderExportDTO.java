package com.huace.trace.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OrderExportDTO {
    @ExcelProperty("订单编号")
    @ColumnWidth(22)
    private String orderNo;

    @ExcelProperty("企业名称")
    @ColumnWidth(20)
    private String enterpriseName;

    @ExcelProperty("证书编号")
    @ColumnWidth(18)
    private String certNo;

    @ExcelProperty("产品名称")
    @ColumnWidth(16)
    private String productName;

    @ExcelProperty("商品名称")
    @ColumnWidth(14)
    private String goodsName;

    @ExcelProperty("包装规格")
    @ColumnWidth(10)
    private String packageSpec;

    @ExcelProperty("重量规格")
    @ColumnWidth(10)
    private String weightSpec;

    @ExcelProperty("标签规格")
    @ColumnWidth(16)
    private String labelSpecName;

    @ExcelProperty("单价(元)")
    @ColumnWidth(10)
    private String unitPrice;

    @ExcelProperty("订购数量(枚)")
    @ColumnWidth(12)
    private Integer quantity;

    @ExcelProperty("总价(元)")
    @ColumnWidth(10)
    private String totalPrice;

    @ExcelProperty("起始身份码")
    @ColumnWidth(14)
    private String serialStart;

    @ExcelProperty("结束身份码")
    @ColumnWidth(14)
    private String serialEnd;

    @ExcelProperty("商品批次")
    @ColumnWidth(16)
    private String productBatch;

    @ExcelProperty("生产时间")
    @ColumnWidth(18)
    private String productionTime;
}
