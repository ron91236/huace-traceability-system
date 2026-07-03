package com.huace.trace.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.mapper.CodePackageItemMapper;
import org.springframework.stereotype.Service;

/**
 * 码包明细批量写入服务（支撑亿级码生成批量插入 MySQL）
 */
@Service
public class CodePackageItemService extends ServiceImpl<CodePackageItemMapper, CodePackageItem> {
}
