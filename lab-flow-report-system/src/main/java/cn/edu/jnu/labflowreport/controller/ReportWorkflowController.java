package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.workflow.dto.ReviewCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.SubmissionCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskCreateRequest;
import cn.edu.jnu.labflowreport.workflow.service.ReportWorkflowService;
import cn.edu.jnu.labflowreport.workflow.vo.ReviewVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportWorkflowController {

    private final ReportWorkflowService reportWorkflowService;

    public ReportWorkflowController(ReportWorkflowService reportWorkflowService) {
        this.reportWorkflowService = reportWorkflowService;
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<TaskVO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("任务创建成功", reportWorkflowService.createTask(user, request));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TaskVO>> listTasks() {
        return ApiResponse.success(reportWorkflowService.listTasks());
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskVO> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(reportWorkflowService.getTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SubmissionVO> submitReport(
            @PathVariable Long taskId,
            @Valid @RequestBody SubmissionCreateRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("提交成功", reportWorkflowService.submitReport(taskId, user, request));
    }

    @GetMapping("/tasks/{taskId}/submissions/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<SubmissionVO>> listMySubmissions(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.listMySubmissions(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/submissions")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<List<SubmissionVO>> listTaskSubmissions(@PathVariable Long taskId) {
        return ApiResponse.success(reportWorkflowService.listTaskSubmissions(taskId));
    }

    @PostMapping("/submissions/{submissionId}/review")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<ReviewVO> reviewSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("批阅成功", reportWorkflowService.reviewSubmission(submissionId, user, request));
    }

    @GetMapping("/submissions/{submissionId}/review")
    public ApiResponse<ReviewVO> getReview(@PathVariable Long submissionId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.getReview(submissionId, user));
    }

    @GetMapping("/tasks/{taskId}/scores/export")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<String> exportTaskScores(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        String csv = reportWorkflowService.exportScoresCsv(taskId, user);
        String filename = "task-" + taskId + "-scores.csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name())
                .body(csv);
    }
}

