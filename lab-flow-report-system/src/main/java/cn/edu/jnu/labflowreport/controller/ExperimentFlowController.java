package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.flow.dto.TaskDeviceRequestCreateRequest;
import cn.edu.jnu.labflowreport.flow.service.ExperimentFlowService;
import cn.edu.jnu.labflowreport.flow.vo.TaskCompletionVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceConfigVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskDeviceRequestVO;
import cn.edu.jnu.labflowreport.flow.vo.TaskProgressVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ExperimentFlowController {

    private final ExperimentFlowService experimentFlowService;

    public ExperimentFlowController(ExperimentFlowService experimentFlowService) {
        this.experimentFlowService = experimentFlowService;
    }

    @GetMapping("/tasks/{taskId}/progress/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TaskProgressVO>> listMyProgress(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listMyProgress(taskId, user));
    }

    @PostMapping(value = "/tasks/{taskId}/progress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskProgressVO> addProgress(
            @PathVariable Long taskId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("记录成功", experimentFlowService.addProgress(taskId, user, content, files));
    }

    @GetMapping("/tasks/{taskId}/completion/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskCompletionVO> getMyCompletion(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.getMyCompletion(taskId, user));
    }

    @PostMapping("/tasks/{taskId}/completion")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskCompletionVO> requestCompletion(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("已提交登记", experimentFlowService.requestCompletion(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/devices")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TaskDeviceConfigVO>> listTaskDevices(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listTaskDevicesForStudent(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/device-requests/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<TaskDeviceRequestVO>> listMyDeviceRequests(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(experimentFlowService.listMyDeviceRequests(taskId, user));
    }

    @PostMapping("/tasks/{taskId}/device-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<TaskDeviceRequestVO> createDeviceRequest(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskDeviceRequestCreateRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("申请已提交", experimentFlowService.createDeviceRequest(taskId, user, request));
    }

    @GetMapping("/progress-attachments/{attachmentId}/download")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadProgressAttachment(@PathVariable Long attachmentId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        ExperimentFlowService.DownloadData data = experimentFlowService.downloadProgressAttachment(attachmentId, user);
        String filename = data.filename() == null ? ("progress-attachment-" + attachmentId) : data.filename();
        String contentType = (data.contentType() == null || data.contentType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : data.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data.bytes());
    }
}
