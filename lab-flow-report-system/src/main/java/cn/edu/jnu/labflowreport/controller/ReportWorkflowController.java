package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.common.export.ExportResponseHelper;
import cn.edu.jnu.labflowreport.workflow.dto.ReviewCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.SubmissionCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskCreateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskStatusUpdateRequest;
import cn.edu.jnu.labflowreport.workflow.dto.TaskTitleUpdateRequest;
import cn.edu.jnu.labflowreport.workflow.service.ReportAttachmentService;
import cn.edu.jnu.labflowreport.workflow.service.ReportWorkflowService;
import cn.edu.jnu.labflowreport.workflow.vo.ReviewVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskAttachmentVO;
import cn.edu.jnu.labflowreport.workflow.vo.TaskVO;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReportWorkflowController {

    private final ReportWorkflowService reportWorkflowService;
    private final ReportAttachmentService reportAttachmentService;

    public ReportWorkflowController(
            ReportWorkflowService reportWorkflowService,
            ReportAttachmentService reportAttachmentService
    ) {
        this.reportWorkflowService = reportWorkflowService;
        this.reportAttachmentService = reportAttachmentService;
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<TaskVO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("任务创建成功", reportWorkflowService.createTask(user, request));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TaskVO>> listTasks() {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.listTasks(user));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<TaskVO> getTask(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.getTaskForUser(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ApiResponse<List<TaskAttachmentVO>> listTaskAttachments(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.listTaskAttachments(taskId, user));
    }

    @PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<List<TaskAttachmentVO>> uploadTaskAttachments(
            @PathVariable Long taskId,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("任务附件上传成功", reportWorkflowService.uploadTaskAttachments(taskId, user, files));
    }

    @DeleteMapping("/tasks/{taskId}/attachments/{attachmentId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteTaskAttachment(@PathVariable Long taskId, @PathVariable Long attachmentId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        reportWorkflowService.deleteTaskAttachment(taskId, attachmentId, user);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/task-attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadTaskAttachment(@PathVariable Long attachmentId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        ReportWorkflowService.DownloadData data = reportWorkflowService.downloadTaskAttachment(attachmentId, user);
        String filename = data.filename() == null ? ("task-attachment-" + attachmentId) : data.filename();
        MediaType contentType = data.contentType() == null || data.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(data.contentType());
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data.bytes());
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        reportWorkflowService.deleteTask(taskId, user);
        return ApiResponse.success("任务已删除", null);
    }

    @PutMapping("/tasks/{taskId}/title")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<TaskVO> updateTaskTitle(@PathVariable Long taskId, @Valid @RequestBody TaskTitleUpdateRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("任务标题已更新", reportWorkflowService.updateTaskTitle(taskId, actor, request));
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

    @PostMapping(value = "/tasks/{taskId}/submissions/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<SubmissionVO> submitReportMultipart(
            @PathVariable Long taskId,
            @RequestParam(value = "contentMd", required = false) String contentMd,
            @RequestParam(value = "confirmEmptyContent", required = false, defaultValue = "false") boolean confirmEmptyContent,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(
                "提交成功",
                reportWorkflowService.submitReportMultipart(taskId, user, contentMd, confirmEmptyContent, files)
        );
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
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success(reportWorkflowService.listTaskSubmissions(taskId, actor));
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
    public ResponseEntity<byte[]> exportTaskScores(@PathVariable Long taskId) throws Exception {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.csv("task-" + taskId + "-scores.csv", reportWorkflowService.exportScoresCsv(taskId, user));
    }

    @GetMapping("/tasks/{taskId}/scores/export/excel")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTaskScoresExcel(@PathVariable Long taskId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ExportResponseHelper.xlsx("task-" + taskId + "-scores.xlsx", reportWorkflowService.exportScoresExcel(taskId, user));
    }

    @PutMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<TaskVO> updateTaskStatus(@PathVariable Long taskId, @Valid @RequestBody TaskStatusUpdateRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", reportWorkflowService.updateTaskStatus(taskId, actor, request));
    }

    @GetMapping("/submissions/{submissionId}/content/download")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadSubmissionContent(@PathVariable Long submissionId) throws IOException {
        AuthenticatedUser user = SecurityUtils.currentUser();
        SubmissionVO submission = reportAttachmentService.getSubmissionForDownload(submissionId, user);

        String student = submission.getStudentUsername() == null ? "student" : submission.getStudentUsername();
        String version = submission.getVersionNo() == null ? "v" : "v" + submission.getVersionNo();
        String filename = "submission-" + submissionId + "-" + student + "-" + version + ".md";

        String content = submission.getContentMd() == null ? "" : submission.getContentMd();

        // Add UTF-8 BOM for Windows tools that mis-detect encoding.
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream(contentBytes.length + 3);
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        out.write(contentBytes);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(out.toByteArray());
    }
}
