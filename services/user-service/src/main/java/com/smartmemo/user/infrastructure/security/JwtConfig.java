package com.smartmemo.user.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置属性。
 * 从 application.yml 中 jwt.* 读取。
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** HMAC-SHA256 签名密钥 */
    private String secret;

    /** Access Token 有效期（秒），默认 900（15 分钟） */
    private long accessTokenExpiration = 900;

    /** Refresh Token 有效期（秒），默认 604800（7 天） */
    private long refreshTokenExpiration = 604800;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public void setAccessTokenExpiration(long accessTokenExpiration) { this.accessTokenExpiration = accessTokenExpiration; }

    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    public void setRefreshTokenExpiration(long refreshTokenExpiration) { this.refreshTokenExpiration = refreshTokenExpiration; }
}
