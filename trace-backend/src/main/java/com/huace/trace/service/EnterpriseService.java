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
    private final TracePageService tracePageService;

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

        String account = enterprise.getLoginAccount() == null ? null : enterprise.getLoginAccount().trim();
        if (!StringUtils.hasText(account)) {
            throw new BusinessException("登录账号不能为空");
        }
        enterprise.setLoginAccount(account);
        assertAccountAvailable(account);

        if (enterprise.getStatus() == null) {
            enterprise.setStatus(1);
        }
        enterpriseMapper.insert(enterprise);

        // 同步创建 sys_user 记录
        SysUser user = new SysUser();
        user.setUsername(account);
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
        String account = enterprise.getLoginAccount() == null ? null : enterprise.getLoginAccount().trim();
        if (StringUtils.hasText(account)) {
            enterprise.setLoginAccount(account);
            // 账号没改就别查：查了会命中自己那一行，变成"保存一次报一次重复"
            Enterprise current = enterpriseMapper.selectById(id);
            if (!account.equals(current == null ? null : current.getLoginAccount())) {
                assertAccountAvailable(account);
            }
        }
        // 处理密码更新
        String rawPassword = StringUtils.hasText(enterprise.getLoginPassword())
                ? enterprise.getLoginPassword()
                : enterprise.getLoginPasswordHash();
        if (StringUtils.hasText(rawPassword) && !rawPassword.startsWith("$2a$")) {
            enterprise.setLoginPasswordHash(passwordEncoder.encode(rawPassword));
        }
        enterpriseMapper.updateById(enterprise);

        // 企业配置（如防伪码输入提示语）变更后清除溯源页缓存，保证扫码页即时生效
        tracePageService.evictAllCache();

        // 同步更新 sys_user
        if (StringUtils.hasText(account)) {
            SysUser existingUser = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnterpriseId, id));
            if (existingUser != null) {
                existingUser.setUsername(account);
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

    /**
     * 登录账号在两张表上各挂一个唯一索引：enterprise.uk_login_account 与 sys_user.uk_username
     * （企业账号会同步建一条 sys_user，而管理端账号也住在那张表里）。
     * 撞上去后 MySQL 抛的那一整段带 ### 与索引名的异常，会被全局兜底原样拼进"系统内部错误: "
     * 回显到弹窗上——管理员看到的就是乱码。所以两处都先查一遍，把"已存在"说成人话。
     */
    private void assertAccountAvailable(String account) {
        if (enterpriseMapper.selectCount(new LambdaQueryWrapper<Enterprise>()
                .eq(Enterprise::getLoginAccount, account)) > 0) {
            throw new BusinessException("登录账号「" + account + "」已存在，请换一个");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, account)) > 0) {
            throw new BusinessException("登录账号「" + account + "」已存在，请换一个");
        }
    }

    @Transactional
    public void delete(Long id) {
        enterpriseMapper.deleteById(id);
        // 删除关联用户
        sysUserMapper.delete(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnterpriseId, id));
    }
}
