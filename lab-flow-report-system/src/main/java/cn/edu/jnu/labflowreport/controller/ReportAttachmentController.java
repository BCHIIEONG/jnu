package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.workflow.service.ReportAttachmentService;
import cn.edu.jnu.labflowreport.workflow.vo.AttachmentVO;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReportAttachmentController {

    private final ReportAttachmentService reportAttachmentService;

    public ReportAttachmentController(ReportAttachmentService reportAttachmentService) {
        this.reportAttachmentService = reportAttachmentService;
    }

    @GetMapping("/submissions/{submissionId}/attachments")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ApiResponse<List<AttachmentVO>> listAttachments(@PathVariable Long submissionId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success(reportAttachmentService.listAttachments(submissionId, user));
    }

    @PostMapping(value = "/submissions/{submissionId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AttachmentVO> uploadAttachment(
            @PathVariable Long submissionId,
            @NotNull @RequestParam("file") MultipartFile file
    ) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        return ApiResponse.success("上传成功", reportAttachmentService.uploadAttachment(submissionId, user, file));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        AuthenticatedUser user = SecurityUtils.currentUser();
        ReportAttachmentService.DownloadData data = reportAttachmentService.downloadAttachment(attachmentId, user);

        String filename = data.filename() == null ? ("attachment-" + attachmentId) : data.filename();
        String contentType = (data.contentType() == null || data.contentType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : data.contentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(data.bytes());
    }
}

