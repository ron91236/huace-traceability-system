package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.SysUser;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.SysUserMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 新建/改名时的登录账号唯一性。账号在两张表上各有一个唯一索引
 * （enterprise.uk_login_account、sys_user.uk_username），不预检就撞库的话，
 * MySQL 那整段带 ### 和索引名的异常会被全局兜底原样拼进"系统内部错误: "回显给管理员，
 * 看起来就是乱码。钉住：
 * 1. 撞在两张表上都要报得出"已存在"，且一条 insert 都不发
 * 2. 改自己的账号不算撞自己
 * 3. 提示里只出现账号本身，不出现 SQL 片段
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseServiceTest {

    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private TracePageService tracePageService;

    private EnterpriseService service;

    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Enterprise.class);
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    @BeforeEach
    void setUp() {
        // 参数顺序必须与 @RequiredArgsConstructor 的字段顺序一致
        service = new EnterpriseService(enterpriseMapper, sysUserMapper, passwordEncoder, tracePageService);
    }

    private Enterprise aNewEnterprise(String account) {
        Enterprise e = new Enterprise();
        e.setName("测试企业");
        e.setLoginAccount(account);
        e.setLoginPassword("huace2026");
        return e;
    }

    @Test
    void 新建时账号已被别的企业占用则拒绝且不写库() {
        when(enterpriseMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(aNewEnterprise("admin")));

        assertTrue(ex.getMessage().contains("admin"), "提示要带上撞车的账号：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("已存在"));
        assertFalse(ex.getMessage().contains("Duplicate"), "不能把 SQL 异常文案漏给用户：" + ex.getMessage());
        verify(enterpriseMapper, never()).insert(any(Enterprise.class));
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void 企业表没占用但账号名已被其他用户占用也要拒绝() {
        // 管理端账号也住在 sys_user：只查 enterprise 就会让"新建企业用了 admin"撞 uk_username，
        // 而那是只有 SQL 文案的一条异常
        when(enterpriseMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(aNewEnterprise("admin")));

        assertTrue(ex.getMessage().contains("已存在"));
        verify(enterpriseMapper, never()).insert(any(Enterprise.class));
    }

    @Test
    void 账号未被占用时照常建企业和同名用户() {
        when(enterpriseMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("huace2026")).thenReturn("$2a$10$hash");
        when(enterpriseMapper.insert(any(Enterprise.class))).thenAnswer(inv -> {
            inv.getArgument(0, Enterprise.class).setId(101L);
            return 1;
        });

        service.create(aNewEnterprise("newco"));

        verify(enterpriseMapper).insert(any(Enterprise.class));
        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(captor.capture());
        assertEquals("newco", captor.getValue().getUsername());
        assertEquals(101L, captor.getValue().getEnterpriseId());
    }

    @Test
    void 查重只按登录账号这一列查() {
        when(enterpriseMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.create(aNewEnterprise("admin")));

        String sql = capturedEnterpriseCountQuery().getSqlSegment();
        assertTrue(sql.toUpperCase().contains("LOGIN_ACCOUNT"), "预检查错列等于没防：" + sql);
    }

    @Test
    void 改资料时不改账号就不必查重复() {
        Enterprise current = new Enterprise();
        current.setId(7L);
        current.setLoginAccount("mu");
        Enterprise form = new Enterprise();
        form.setName("改名后的企业");
        form.setLoginAccount("mu");
        when(enterpriseMapper.selectById(7L)).thenReturn(current);
        when(sysUserMapper.selectOne(any())).thenReturn(null);

        assertDoesNotThrow(() -> service.update(7L, form));

        // 账号没变还去查唯一性，就会查到它自己那一行，"保存一次报一次重复"
        verify(enterpriseMapper, never()).selectCount(any());
        verify(enterpriseMapper).updateById(any(Enterprise.class));
    }

    @Test
    void 改账号撞上别的企业时拒绝() {
        Enterprise current = new Enterprise();
        current.setId(7L);
        current.setLoginAccount("mu");
        Enterprise form = new Enterprise();
        form.setLoginAccount("other");
        when(enterpriseMapper.selectById(7L)).thenReturn(current);
        when(enterpriseMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(7L, form));

        assertTrue(ex.getMessage().contains("已存在"));
        verify(enterpriseMapper, never()).updateById(any(Enterprise.class));
    }

    private LambdaQueryWrapper<Enterprise> capturedEnterpriseCountQuery() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Enterprise>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(enterpriseMapper).selectCount(captor.capture());
        return captor.getValue();
    }
}
