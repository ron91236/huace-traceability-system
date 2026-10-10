package com.huace.trace.config;

import com.huace.trace.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/trace/**").permitAll()
                .requestMatchers("/api/dl/**").permitAll()
                // 合格证公开查询
                .requestMatchers("/api/hgz/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/poster/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 华测管理端
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 数字标签：管理员（全局只读）与企业用户均可访问
                .requestMatchers("/api/enterprise/dl/**").hasAnyRole("ADMIN", "ENTERPRISE")
                // 承诺达标合格证：管理员（全局查看/作废）与企业用户均可访问
                .requestMatchers("/api/enterprise/hgz/**").hasAnyRole("ADMIN", "ENTERPRISE")
                // 企业端
                .requestMatchers("/api/enterprise/**").hasRole("ENTERPRISE")
                // 其他需要认证
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // 未登录/登录过期必须返回401，前端据此跳登录页；默认403会让过期会话停留在业务页报错
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"msg\":\"无权限访问该功能\",\"data\":null}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
