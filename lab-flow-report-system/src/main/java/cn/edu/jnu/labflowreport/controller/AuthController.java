package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.dto.CurrentUserResponse;
import cn.edu.jnu.labflowreport.auth.dto.LoginRequest;
import cn.edu.jnu.labflowreport.auth.dto.LoginResponse;
import cn.edu.jnu.labflowreport.auth.service.AuthService;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.success(authService.me());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("登出成功", null);
    }
}

