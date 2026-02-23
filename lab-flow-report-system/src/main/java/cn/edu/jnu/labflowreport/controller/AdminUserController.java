package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.AdminUserCreateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserImportResult;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserResetPasswordRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserSetRolesRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserUpdateRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminUserVO;
import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.admin.service.AdminUserService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ApiResponse<PageResult<AdminUserVO>> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminUserService.listUsers(q, roleCode, enabled, departmentId, classId, page, size));
    }

    @PostMapping("/users")
    public ApiResponse<AdminUserVO> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminUserService.createUser(actor, request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<AdminUserVO> updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminUserService.updateUser(actor, id, request));
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) AdminUserResetPasswordRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        String newPassword = request == null ? null : request.newPassword();
        adminUserService.resetPassword(actor, id, newPassword);
        return ApiResponse.success("重置成功", null);
    }

    @PutMapping("/users/{id}/roles")
    public ApiResponse<Void> setRoles(@PathVariable Long id, @Valid @RequestBody AdminUserSetRolesRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminUserService.setUserRoles(actor, id, request.roleCodes());
        return ApiResponse.success("设置成功", null);
    }

    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminUserImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("导入完成", adminUserService.importUsers(actor, file));
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        String csv = adminUserService.exportUsersCsv(actor);
        return csvResponse("users.csv", csv);
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) throws Exception {
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(csvBytes.length + 3);
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.write(csvBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(out.toByteArray());
    }
}

