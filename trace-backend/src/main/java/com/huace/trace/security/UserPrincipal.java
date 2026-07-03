package com.huace.trace.security;

import lombok.Data;

@Data
public class UserPrincipal {
    private Long userId;
    private String username;
    private String userType;
    private Long enterpriseId;
    private String accountLevel;

    public UserPrincipal(Long userId, String username, String userType) {
        this.userId = userId;
        this.username = username;
        this.userType = userType;
    }

    public UserPrincipal(Long userId, String username, String userType, Long enterpriseId, String accountLevel) {
        this.userId = userId;
        this.username = username;
        this.userType = userType;
        this.enterpriseId = enterpriseId;
        this.accountLevel = accountLevel;
    }
}
