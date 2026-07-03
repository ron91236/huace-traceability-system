package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.SysUser;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final EnterpriseMapper enterpriseMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<Enterprise> list(int page, int size, String keyword) {
        LambdaQueryWrapper<Enterprise> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Enterprise::getName, keyword)
                    .or().like(Enterprise::getContact, keyword)
                    .or().like(Enterprise::getPhone, keyword);
        }
        wrapper.orderByDesc(Enterprise::getId);
        Page<Enterprise> result = enterpriseMapper.selectPage(new Page<>(page, size), wrapper);
        // 清除密码
        result.getRecords().forEach(e -> { e.setLoginPasswordHash(null); e.setLoginPassword(null); });
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    public List<Enterprise> all() {
        List<Enterprise> list = enterpriseMapper.selectList(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getStatus, 1)
                        .orderByDesc(Enterprise::getId));
        list.forEach(e -> { e.setLoginPasswordHash(null); e.setLoginPassword(null); });
        return list;
    }

    public Enterprise getById(Long id) {
        Enterprise e = enterpriseMapper.selectById(id);
        if (e != null) { e.setLoginPasswordHash(null); e.setLoginPassword(null); }
        return e;
    }

    @Transactional
    public void create(Enterprise enterprise) {
        // 优先使用前端传来的 loginPassword，兼容 loginPasswordHash
        String rawPassword = StringUtils.hasText(enterprise.getLoginPassword())
                ? enterprise.getLoginPassword()
                : enterprise.getLoginPasswordHash();

        if (StringUtils.hasText(rawPassword)) {
            String encoded = passwordEncoder.encode(rawPassword);
            enterprise.setLoginPasswordHash(encoded);
        } else {
            throw new BusinessException("登录密码不能为空");
        }

        if (!StringUtils.hasText(enterprise.getLoginAccount())) {
            throw new BusinessException("登录账号不能为空");
        }

        if (enterprise.getStatus() == null) {
            enterprise.setStatus(1);
        }
        enterpriseMapper.insert(enterprise);

        // 同步创建 sys_user 记录
        SysUser user = new SysUser();
        user.setUsername(enterprise.getLoginAccount());
        user.setPasswordHash(enterprise.getLoginPasswordHash());
        user.setNickname(enterprise.getName());
        user.setPhone(enterprise.getPhone());
        user.setEmail(enterprise.getEmail());
        user.setUserType("enterprise");
        user.setEnterpriseId(enterprise.getId());
        user.setRoleId(2L); // 企业角色
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    @Transactional
    public void update(Long id, Enterprise enterprise) {
        enterprise.setId(id);
        // 处理密码更新
        String rawPassword = StringUtils.hasText(enterprise.getLoginPassword())
                ? enterprise.getLoginPassword()
                : enterprise.getLoginPasswordHash();
        if (StringUtils.hasText(rawPassword) && !rawPassword.startsWith("$2a$")) {
            enterprise.setLoginPasswordHash(passwordEncoder.encode(rawPassword));
        }
        enterpriseMapper.updateById(enterprise);

        // 同步更新 sys_user
        if (StringUtils.hasText(enterprise.getLoginAccount())) {
            SysUser existingUser = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnterpriseId, id));
            if (existingUser != null) {
                existingUser.setUsername(enterprise.getLoginAccount());
                if (StringUtils.hasText(enterprise.getLoginPasswordHash())) {
                    existingUser.setPasswordHash(enterprise.getLoginPasswordHash());
                }
                existingUser.setNickname(enterprise.getName());
                existingUser.setPhone(enterprise.getPhone());
                existingUser.setEmail(enterprise.getEmail());
                sysUserMapper.updateById(existingUser);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        enterpriseMapper.deleteById(id);
        // 删除关联用户
        sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnterpriseId, id));
    }
}
