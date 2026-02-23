package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.AdminClassRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminClassVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentVO;
import cn.edu.jnu.labflowreport.admin.service.AdminOrgService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrgController {

    private final AdminOrgService adminOrgService;

    public AdminOrgController(AdminOrgService adminOrgService) {
        this.adminOrgService = adminOrgService;
    }

    @GetMapping("/departments")
    public ApiResponse<List<AdminDepartmentVO>> listDepartments() {
        return ApiResponse.success(adminOrgService.listDepartments());
    }

    @PostMapping("/departments")
    public ApiResponse<AdminDepartmentVO> createDepartment(@Valid @RequestBody AdminDepartmentRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminOrgService.createDepartment(actor, request));
    }

    @PutMapping("/departments/{id}")
    public ApiResponse<AdminDepartmentVO> updateDepartment(@PathVariable Long id, @Valid @RequestBody AdminDepartmentRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminOrgService.updateDepartment(actor, id, request));
    }

    @DeleteMapping("/departments/{id}")
    public ApiResponse<Void> deleteDepartment(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminOrgService.deleteDepartment(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/departments/export")
    public ResponseEntity<byte[]> exportDepartments() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("departments.csv", adminOrgService.exportDepartmentsCsv(actor));
    }

    @GetMapping("/classes")
    public ApiResponse<List<AdminClassVO>> listClasses(@RequestParam(required = false) Long departmentId) {
        return ApiResponse.success(adminOrgService.listClasses(departmentId));
    }

    @PostMapping("/classes")
    public ApiResponse<AdminClassVO> createClass(@Valid @RequestBody AdminClassRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminOrgService.createClass(actor, request));
    }

    @PutMapping("/classes/{id}")
    public ApiResponse<AdminClassVO> updateClass(@PathVariable Long id, @Valid @RequestBody AdminClassRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminOrgService.updateClass(actor, id, request));
    }

    @DeleteMapping("/classes/{id}")
    public ApiResponse<Void> deleteClass(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminOrgService.deleteClass(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/classes/export")
    public ResponseEntity<byte[]> exportClasses() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("classes.csv", adminOrgService.exportClassesCsv(actor));
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

