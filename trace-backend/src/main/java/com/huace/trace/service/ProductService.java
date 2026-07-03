package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Product;
import com.huace.trace.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    public PageResult<Product> list(int page, int size, String keyword) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) w.like(Product::getName, keyword);
        w.orderByDesc(Product::getId);
        Page<Product> r = productMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Product p) { productMapper.insert(p); }
    public void update(Long id, Product p) { p.setId(id); productMapper.updateById(p); }
    public void delete(Long id) { productMapper.deleteById(id); }
}
package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Product;
import com.huace.trace.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    public PageResult<Product> list(int page, int size, String keyword) {
        LambdaQueryWrapper<Product> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) w.like(Product::getName, keyword);
        w.orderByDesc(Product::getId);
        Page<Product> r = productMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public List<Product> all() {
        return productMapper.selectList(new LambdaQueryWrapper<Product>().orderByDesc(Product::getId));
    }

    public void create(Product p) { productMapper.insert(p); }
    public void update(Long id, Product p) { p.setId(id); productMapper.updateById(p); }
    public void delete(Long id) { productMapper.deleteById(id); }
}
