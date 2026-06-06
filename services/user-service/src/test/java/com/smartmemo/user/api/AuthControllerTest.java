package com.smartmemo.user.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartmemo.user.application.AuthApplicationService;
import com.smartmemo.user.application.AuthApplicationService.LoginResult;
import com.smartmemo.user.application.AuthApplicationService.RegisterResult;
import com.smartmemo.user.api.dto.LoginRequest;
import com.smartmemo.user.api.dto.RefreshRequest;
import com.smartmemo.user.api.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 接口测试。
 */
@WebMvcTest(AuthController.class)
@Import(com.smartmemo.user.config.SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthApplicationService authService;

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authService.register(anyString(), anyString(), any(), any()))
                .thenReturn(RegisterResult.success(userId, "newuser"));

        var req = new RegisterRequest("newuser", "Password123", "a@b.com", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.username").value("newuser"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        when(authService.login(anyString(), anyString(), any(), any()))
                .thenReturn(LoginResult.success("access-token-xxx", "refresh-token-xxx", 900));

        var req = new LoginRequest("testuser", "Password123", "web", "Chrome");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-xxx"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-xxx"))
                .andExpect(jsonPath("$.data.expiresIn").value(900));
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        when(authService.refresh(anyString()))
                .thenReturn(LoginResult.success("new-access", "new-refresh", 900));

        var req = new RefreshRequest("valid-refresh-token");
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

}
