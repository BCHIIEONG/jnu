package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.AdminAuditLogVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminDeviceRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminDeviceVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminRoleVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminSemesterRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminSemesterVO;
import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.admin.service.AdminResourceService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
public class AdminResourceController {

    private final AdminResourceService adminResourceService;

    public AdminResourceController(AdminResourceService adminResourceService) {
        this.adminResourceService = adminResourceService;
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleVO>> listRoles() {
        return ApiResponse.success(adminResourceService.listRoles());
    }

    @GetMapping("/lab-rooms")
    public ApiResponse<List<AdminLabRoomVO>> listLabRooms() {
        return ApiResponse.success(adminResourceService.listLabRooms());
    }

    @PostMapping("/lab-rooms")
    public ApiResponse<AdminLabRoomVO> createLabRoom(@Valid @RequestBody AdminLabRoomRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminResourceService.createLabRoom(actor, request));
    }

    @PutMapping("/lab-rooms/{id}")
    public ApiResponse<AdminLabRoomVO> updateLabRoom(@PathVariable Long id, @Valid @RequestBody AdminLabRoomRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminResourceService.updateLabRoom(actor, id, request));
    }

    @DeleteMapping("/lab-rooms/{id}")
    public ApiResponse<Void> deleteLabRoom(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminResourceService.deleteLabRoom(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/lab-rooms/export")
    public ResponseEntity<byte[]> exportLabRooms() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("lab-rooms.csv", adminResourceService.exportLabRoomsCsv(actor));
    }

    @GetMapping("/devices")
    public ApiResponse<List<AdminDeviceVO>> listDevices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(adminResourceService.listDevices(q, status));
    }

    @PostMapping("/devices")
    public ApiResponse<AdminDeviceVO> createDevice(@Valid @RequestBody AdminDeviceRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminResourceService.createDevice(actor, request));
    }

    @PutMapping("/devices/{id}")
    public ApiResponse<AdminDeviceVO> updateDevice(@PathVariable Long id, @Valid @RequestBody AdminDeviceRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminResourceService.updateDevice(actor, id, request));
    }

    @DeleteMapping("/devices/{id}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminResourceService.deleteDevice(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/devices/export")
    public ResponseEntity<byte[]> exportDevices() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("devices.csv", adminResourceService.exportDevicesCsv(actor));
    }

    @GetMapping("/semesters")
    public ApiResponse<List<AdminSemesterVO>> listSemesters() {
        return ApiResponse.success(adminResourceService.listSemesters());
    }

    @PostMapping("/semesters")
    public ApiResponse<AdminSemesterVO> createSemester(@Valid @RequestBody AdminSemesterRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", adminResourceService.createSemester(actor, request));
    }

    @PutMapping("/semesters/{id}")
    public ApiResponse<AdminSemesterVO> updateSemester(@PathVariable Long id, @Valid @RequestBody AdminSemesterRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", adminResourceService.updateSemester(actor, id, request));
    }

    @DeleteMapping("/semesters/{id}")
    public ApiResponse<Void> deleteSemester(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        adminResourceService.deleteSemester(actor, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/semesters/export")
    public ResponseEntity<byte[]> exportSemesters() throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("semesters.csv", adminResourceService.exportSemestersCsv(actor));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AdminAuditLogVO>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminResourceService.listAuditLogs(action, actorUsername, targetType, from, to, page, size));
    }

    @GetMapping("/audit-logs/export")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) throws Exception {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return csvResponse("audit-logs.csv", adminResourceService.exportAuditLogsCsv(actor, action, actorUsername, targetType, from, to));
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

