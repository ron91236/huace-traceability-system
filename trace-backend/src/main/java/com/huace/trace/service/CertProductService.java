package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.CertProduct;
import com.huace.trace.entity.Product;
import com.huace.trace.mapper.CertProductMapper;
import com.huace.trace.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CertProductService {

    private final CertProductMapper certProductMapper;
    private final ProductMapper productMapper;

    public List<CertProduct> listByCertId(Long certId) {
        List<CertProduct> products = certProductMapper.selectList(
                new LambdaQueryWrapper<CertProduct>()
                        .eq(CertProduct::getCertId, certId)
                        .orderByAsc(CertProduct::getId));
        products.forEach(cp -> {
            if (cp.getProductId() != null) {
                Product p = productMapper.selectById(cp.getProductId());
                if (p != null) {
                    cp.setProductName(p.getName());
                    cp.setProductDescription(p.getDescription());
                }
            }
        });
        return products;
    }

    public void addProduct(CertProduct certProduct) {
        if (certProduct.getProductId() != null) {
            Product p = productMapper.selectById(certProduct.getProductId());
            if (p != null) certProduct.setProductName(p.getName());
        }
        // 设置剩余产量等于总产量
        certProduct.setRemainingProduction(certProduct.getTotalProduction());
        certProductMapper.insert(certProduct);
    }

    public void removeProduct(Long id) {
        certProductMapper.deleteById(id);
    }

    /**
     * 产能校验并扣减
     * @param certId 证书ID
     * @param weightTons 订单重量(吨)
     * @return true=产能充足(已扣减), false=产能不足
     */
    @Transactional
    public boolean checkAndDeductProduction(Long certId, BigDecimal weightTons) {
        if (weightTons == null || weightTons.compareTo(BigDecimal.ZERO) <= 0) {
            return false; // 无重量信息或0重量，需人工审核
        }

        List<CertProduct> products = certProductMapper.selectList(
                new LambdaQueryWrapper<CertProduct>()
                        .eq(CertProduct::getCertId, certId));

        if (products.isEmpty()) return false; // 无证书产品配置，需人工审核

        // 汇总所有产品的剩余产量
        BigDecimal totalRemaining = products.stream()
                .map(CertProduct::getRemainingProduction)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRemaining.compareTo(weightTons) >= 0) {
            // 产能充足，按产品顺序依次扣减
            BigDecimal remaining = weightTons;
            for (CertProduct cp : products) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal currentRemaining = cp.getRemainingProduction() != null ? cp.getRemainingProduction() : BigDecimal.ZERO;
                BigDecimal deduct = remaining.min(currentRemaining);
                cp.setRemainingProduction(currentRemaining.subtract(deduct));
                certProductMapper.updateById(cp);
                remaining = remaining.subtract(deduct);
            }
            return true;
        }
        return false; // 产能不足
    }
}
