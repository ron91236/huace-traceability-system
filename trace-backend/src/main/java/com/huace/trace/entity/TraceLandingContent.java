package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 落地页区块内容：某个企业在某个模板的某个区块上填的内容。
 *
 * <p>区块有哪些、叫什么、什么顺序都归模板（config_json.landingBlocks），这里只是内容。
 * content 的形态按 block_key 变：richtext 存 HTML，video 存单个 URL，promoImages 存
 * URL 的 JSON 数组字符串；antiFake 不写行（数据由系统按码实时取，见 spec §6.2）。
 * 「企业未填」= 无行，与长图内容同一套语义。
 */
@Data
@TableName("trace_landing_content")
public class TraceLandingContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Long enterpriseId;
    /** 对应 config_json.landingBlocks[].key，固定枚举 */
    private String blockKey;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
