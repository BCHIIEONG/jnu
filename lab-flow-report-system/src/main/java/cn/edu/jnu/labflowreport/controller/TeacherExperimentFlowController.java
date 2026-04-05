package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.flow.dto.TaskDeviceConfigItemRequest;
import cn.edu.jnu.labflowreport.flow.service.ExperimentFlowService;
import cn.edu.jnu.labflowreport.flow.vo.TaskCompletionVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceConfigVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceRequestVO;
import cn.edu.jnu.labflowreport.flow.vo.TeacherTaskProgressDetailVO;
import cn.edu.jnu.labflowreport.flow.vo.TeacherTaskProgressStudentVO;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherExperimentFlowController {

    private final ExperimentFlowService experimentFlowService;

    public TeacherExperimentFlowController(ExperimentFlowService experimentFlowService) {
        this.experimentFlowService = experimentFlowService;
    }

    @GetMapping("/tasks/{taskId}/progress")
    public ApiResponse<List<TeacherTaskProgressStudentVO>> listTaskProgress(
            @PathVariable Long taskId,
            @RequestParam(required = false) String q
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listTeacherTaskProgress(taskId, user, q));
    }

    @GetMapping("/tasks/{taskId}/progress/{studentId}")
    public ApiResponse<TeacherTaskProgressDetailVO> getTaskProgressDetail(
            @PathVariable Long taskId,
            @PathVariable Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.getTeacherTaskProgressDetail(taskId, studentId, user));
    }

    @PostMapping("/tasks/{taskId}/completion/{studentId}/confirm")
    public ApiResponse<TaskCompletionVO> confirmCompletion(
            @PathVariable Long taskId,
            @PathVariable Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已确认登记", experimentFlowService.confirmCompletion(taskId, studentId, user));
    }

    @PostMapping("/tasks/{taskId}/completion/{studentId}/direct-confirm")
    public ApiResponse<TaskCompletionVO> directConfirmCompletion(
            @PathVariable Long taskId,
            @PathVariable Long studentId
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已直接登记完成", experimentFlowService.directConfirmCompletion(taskId, studentId, user));
    }

    @GetMapping("/tasks/{taskId}/devices")
    public ApiResponse<List<TaskDeviceConfigVO>> listTaskDevices(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listTaskDevicesForTeacher(taskId, user));
    }

    @PutMapping("/tasks/{taskId}/devices")
    public ApiResponse<List<TaskDeviceConfigVO>> updateTaskDevices(
            @PathVariable Long taskId,
            @Valid @RequestBody List<@Valid TaskDeviceConfigItemRequest> request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("设置成功", experimentFlowService.updateTaskDevices(taskId, user, request));
    }

    @GetMapping("/tasks/{taskId}/device-requests")
    public ApiResponse<List<TaskDeviceRequestVO>> listTaskDeviceRequests(
            @PathVariable Long taskId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listTeacherDeviceRequests(taskId, user, status, q));
    }

    @PostMapping("/device-requests/{requestId}/approve")
    public ApiResponse<TaskDeviceRequestVO> approveDeviceRequest(@PathVariable Long requestId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("审批通过", experimentFlowService.approveDeviceRequest(requestId, user));
    }

    @PostMapping("/device-requests/{requestId}/reject")
    public ApiResponse<TaskDeviceRequestVO> rejectDeviceRequest(@PathVariable Long requestId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已驳回", experimentFlowService.rejectDeviceRequest(requestId, user));
    }

    @PostMapping("/device-requests/{requestId}/checkout")
    public ApiResponse<TaskDeviceRequestVO> checkoutDeviceRequest(@PathVariable Long requestId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已登记借出", experimentFlowService.checkoutDeviceRequest(requestId, user));
    }

    @PostMapping("/device-requests/{requestId}/return")
    public ApiResponse<TaskDeviceRequestVO> returnDeviceRequest(@PathVariable Long requestId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已登记归还", experimentFlowService.returnDeviceRequest(requestId, user));
    }

    @GetMapping("/tasks/{taskId}/device-requests/export")
    public ResponseEntity<byte[]> exportTaskDeviceRequests(@PathVariable Long taskId) throws IOException {
        AuthenticatedUser user = SecurityUtils.currentUser();
        String csv = experimentFlowService.exportTaskDeviceRequestsCsv(taskId, user);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(csvBytes.length + 3);
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.write(csvBytes);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"task-" + taskId + "-device-requests.csv\"")
                .body(out.toByteArray());
    }
}
