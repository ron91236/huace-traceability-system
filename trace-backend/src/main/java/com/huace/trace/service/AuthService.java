package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.SysUser;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.SysUserMapper;
import com.huace.trace.security.JwtTokenProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int LOGIN_MAX_FAILS = 5;
    private static final Duration LOGIN_LOCK_WINDOW = Duration.ofMinutes(15);

    private final SysUserMapper sysUserMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public Map<String, Object> login(String username, String password, String loginType, String clientIp) {
        String failKey = "loginFail:" + clientIp + ":" + (username == null ? "" : username.trim());
        try {
            String fails = redisTemplate.opsForValue().get(failKey);
            if (fails != null && Integer.parseInt(fails) >= LOGIN_MAX_FAILS) {
                throw new BusinessException("登录失败次数过多，请15分钟后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("登录限流Redis查询失败，跳过限流: {}", e.getMessage());
        }
        try {
            Map<String, Object> result = doLogin(username, password, loginType);
            try {
                redisTemplate.delete(failKey);
            } catch (Exception e) {
                log.warn("登录限流Redis清理失败: {}", e.getMessage());
            }
            return result;
        } catch (BusinessException e) {
            try {
                Long fails = redisTemplate.opsForValue().increment(failKey);
                if (fails != null && fails == 1L) {
                    redisTemplate.expire(failKey, LOGIN_LOCK_WINDOW);
                }
            } catch (Exception ex) {
                log.warn("登录失败计数写入Redis失败: {}", ex.getMessage());
            }
            throw e;
        }
    }

    private Map<String, Object> doLogin(String username, String password, String loginType) {
        // 统一登录：先查管理员，再查企业
        if ("admin".equals(loginType)) {
            return adminLogin(username, password);
        } else if ("enterprise".equals(loginType)) {
            return enterpriseLogin(username, password);
        }
        // 未指定loginType时，自动识别
        try {
            return adminLogin(username, password);
        } catch (Exception e) {
            return enterpriseLogin(username, password);
        }
    }

    private Map<String, Object> adminLogin(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getUserType, "admin")
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getUserType());
        return buildLoginResult(token, user.getId(), user.getUsername(), user.getNickname(),
                user.getUserType(), null, null, null, null);
    }

    private Map<String, Object> enterpriseLogin(String username, String password) {
        // 企业端通过 login_account 登录
        Enterprise enterprise = enterpriseMapper.selectOne(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getLoginAccount, username)
        );
        if (enterprise == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (enterprise.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, enterprise.getLoginPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }

        // 企业端 JWT 中存储企业ID + accountLevel
        Long userId = enterprise.getId();
        String accountLevel = enterprise.getAccountLevel() != null ? enterprise.getAccountLevel() : "standalone";
        String token = jwtTokenProvider.generateToken(userId, username, "enterprise", enterprise.getId(), accountLevel);

        // 母账号返回子企业列表
        List<Map<String, Object>> childEnterprises = null;
        Long parentEnterpriseId = null;
        if ("master".equals(accountLevel)) {
            List<Enterprise> children = enterpriseMapper.selectList(
                    new LambdaQueryWrapper<Enterprise>()
                            .eq(Enterprise::getParentId, enterprise.getId())
                            .select(Enterprise::getId, Enterprise::getName));
            childEnterprises = children.stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                return m;
            }).collect(Collectors.toList());
        } else if ("child".equals(accountLevel)) {
            parentEnterpriseId = enterprise.getParentId();
        }

        return buildLoginResult(token, userId, username, enterprise.getName(),
                "enterprise", enterprise.getId(), enterprise.getName(), accountLevel, childEnterprises);
    }

    private Map<String, Object> buildLoginResult(String token, Long userId, String username,
                                                   String nickname, String userType,
                                                   Long enterpriseId, String enterpriseName,
                                                   String accountLevel, List<Map<String, Object>> childEnterprises) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("username", username);
        user.put("nickname", nickname);
        user.put("userType", userType);
        user.put("enterpriseId", enterpriseId);
        user.put("enterpriseName", enterpriseName);
        user.put("accountLevel", accountLevel);
        user.put("childEnterprises", childEnterprises);
        result.put("user", user);
        return result;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
        private String loginType; // admin or enterprise
    }
}
