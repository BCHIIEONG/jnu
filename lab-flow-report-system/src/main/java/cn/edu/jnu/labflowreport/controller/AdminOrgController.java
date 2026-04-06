package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.AdminClassRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminClassVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminDepartmentVO;
import cn.edu.jnu.labflowreport.admin.service.AdminOrgService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.common.export.ExportResponseHelper;
import jakarta.validation.Valid;
import java.util.List;
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
        return ExportResponseHelper.csv("departments.csv", adminOrgService.exportDepartmentsCsv(actor));
    }

    @GetMapping("/departments/export/excel")
    public ResponseEntity<byte[]> exportDepartmentsExcel() {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("departments.xlsx", adminOrgService.exportDepartmentsExcel(actor));
    }

    @GetMapping("/classes")
    public ApiResponse<List<AdminClassVO>> listClasses(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.success(adminOrgService.listClasses(departmentId, grade, q));
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
        return ExportResponseHelper.csv("classes.csv", adminOrgService.exportClassesCsv(actor));
    }

    @GetMapping("/classes/export/excel")
    public ResponseEntity<byte[]> exportClassesExcel() {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("classes.xlsx", adminOrgService.exportClassesExcel(actor));
    }
}
