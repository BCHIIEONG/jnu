package cn.edu.jnu.labflowreport.auth.service;

import cn.edu.jnu.labflowreport.auth.dto.CurrentUserResponse;
import cn.edu.jnu.labflowreport.auth.dto.LoginRequest;
import cn.edu.jnu.labflowreport.auth.dto.LoginResponse;
import cn.edu.jnu.labflowreport.auth.dto.UserProfile;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.JwtService;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUserEntity user = sysUserMapper.findByUsername(request.username());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        List<String> roles = sysUserMapper.findRoleCodesByUserId(user.getId());
        String token = jwtService.createToken(user.getId(), user.getUsername(), roles);
        Instant expiresAt = jwtService.expiresAt();
        UserProfile profile = new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), roles);
        return new LoginResponse(token, "Bearer", expiresAt, profile);
    }

    public CurrentUserResponse me() {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        SysUserEntity user = sysUserMapper.selectById(principal.userId());
        if (user == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已失效");
        }
        List<String> roles = sysUserMapper.findRoleCodesByUserId(user.getId());
        return new CurrentUserResponse(new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(), roles));
    }
}

