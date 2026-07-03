package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody AuthService.LoginRequest request) {
        return Result.ok(authService.login(request.getUsername(), request.getPassword(), request.getLoginType()));
    }
}
