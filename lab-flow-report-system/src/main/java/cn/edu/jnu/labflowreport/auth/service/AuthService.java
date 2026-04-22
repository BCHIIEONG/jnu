package cn.edu.jnu.labflowreport.auth.service;

import cn.edu.jnu.labflowreport.auth.dto.CurrentUserResponse;
import cn.edu.jnu.labflowreport.auth.dto.ForceChangePasswordRequest;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String DEFAULT_PASSWORD = "123456";

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
        UserProfile profile = toProfile(user, roles);
        return new LoginResponse(token, "Bearer", expiresAt, profile);
    }

    public CurrentUserResponse me() {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        SysUserEntity user = sysUserMapper.selectById(principal.userId());
        if (user == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已失效");
        }
        List<String> roles = sysUserMapper.findRoleCodesByUserId(user.getId());
        return new CurrentUserResponse(toProfile(user, roles));
    }

    public void forceChangePassword(ForceChangePasswordRequest request) {
        AuthenticatedUser principal = SecurityUtils.currentUser();
        SysUserEntity user = sysUserMapper.selectById(principal.userId());
        if (user == null || Boolean.FALSE.equals(user.getEnabled())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "用户不存在或已失效");
        }

        List<String> roles = sysUserMapper.findRoleCodesByUserId(user.getId());
        if (!isStudentOnly(roles) || !Boolean.TRUE.equals(user.getMustChangePassword())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "当前账号不需要强制修改密码");
        }

        String username = request.username() == null ? null : request.username().trim();
        String displayName = request.displayName() == null ? null : request.displayName().trim();
        if (!Objects.equals(username, user.getUsername()) || !Objects.equals(displayName, user.getDisplayName())) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "账号信息校验失败，请填写当前登录账号对应的学号和姓名");
        }

        String newPassword = request.newPassword() == null ? "" : request.newPassword();
        String confirmPassword = request.confirmPassword() == null ? "" : request.confirmPassword();
        if (!Objects.equals(newPassword, confirmPassword)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "两次输入的新密码不一致");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "新密码长度至少 6 位");
        }
        if (DEFAULT_PASSWORD.equals(newPassword)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "新密码不能使用默认密码");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    private UserProfile toProfile(SysUserEntity user, List<String> roles) {
        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                roles == null ? List.of() : roles,
                Boolean.TRUE.equals(user.getMustChangePassword()) && isStudentOnly(roles)
        );
    }

    private boolean isStudentOnly(List<String> roles) {
        return roles != null
                && roles.contains("ROLE_STUDENT")
                && !roles.contains("ROLE_TEACHER")
                && !roles.contains("ROLE_ADMIN");
    }
}
