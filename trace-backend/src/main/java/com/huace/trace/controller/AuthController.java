package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AuthService.LoginRequest request,
                                             HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request.getUsername(), request.getPassword(),
                request.getLoginType(), getClientIp(httpRequest)));
    }

    /** 优先取 Nginx 注入的 X-Real-IP（来自 $remote_addr，防伪造），回退到直连地址 */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) return ip.trim();
        return request.getRemoteAddr();
    }
}
