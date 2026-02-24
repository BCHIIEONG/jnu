package cn.edu.jnu.labflowreport.workflow.service;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.storage.FileStorageService;
import cn.edu.jnu.labflowreport.workflow.vo.AttachmentVO;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportAttachmentService {

    private final ReportSubmissionMapper submissionMapper;
    private final ReportAttachmentMapper attachmentMapper;
    private final FileStorageService storageService;

    public ReportAttachmentService(
            ReportSubmissionMapper submissionMapper,
            ReportAttachmentMapper attachmentMapper,
            FileStorageService storageService
    ) {
        this.submissionMapper = submissionMapper;
        this.attachmentMapper = attachmentMapper;
        this.storageService = storageService;
    }

    public List<AttachmentVO> listAttachments(Long submissionId, AuthenticatedUser user) {
        ensureCanAccessSubmission(submissionId, user);
        return attachmentMapper.findBySubmissionId(submissionId).stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public AttachmentVO uploadAttachment(Long submissionId, AuthenticatedUser user, MultipartFile file) {
        // For now: only the student (owner) can upload.
        if (!user.roleCodes().contains("ROLE_STUDENT")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只有学生可以上传附件");
        }
        ensureOwner(submissionId, user);

        String filePath = storageService.saveReportAttachment(submissionId, file);

        ReportAttachmentEntity entity = new ReportAttachmentEntity();
        entity.setSubmissionId(submissionId);
        entity.setFileName(Objects.toString(file.getOriginalFilename(), "attachment"));
        entity.setFilePath(filePath);
        entity.setFileSize(file.getSize());
        entity.setContentType(file.getContentType());
        entity.setUploadedAt(LocalDateTime.now());
        attachmentMapper.insert(entity);

        AttachmentVO vo = toVo(entity);
        vo.setId(entity.getId());
        return vo;
    }

    public DownloadData downloadAttachment(Long attachmentId, AuthenticatedUser user) {
        ReportAttachmentEntity entity = attachmentMapper.selectById(attachmentId);
        if (entity == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "附件不存在");
        }
        ensureCanAccessSubmission(entity.getSubmissionId(), user);
        byte[] bytes = storageService.readBytes(entity.getFilePath());
        return new DownloadData(entity.getFileName(), entity.getContentType(), bytes);
    }

    public SubmissionVO getSubmissionForDownload(Long submissionId, AuthenticatedUser user) {
        SubmissionVO submission = submissionMapper.findSubmissionById(submissionId);
        if (submission == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }
        ensureCanAccessSubmission(submissionId, user);
        return submission;
    }

    private void ensureCanAccessSubmission(Long submissionId, AuthenticatedUser user) {
        if (user.roleCodes().contains("ROLE_TEACHER") || user.roleCodes().contains("ROLE_ADMIN")) {
            return;
        }
        ensureOwner(submissionId, user);
    }

    private void ensureOwner(Long submissionId, AuthenticatedUser user) {
        Long ownerId = submissionMapper.findStudentIdBySubmissionId(submissionId);
        if (ownerId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }
        if (!ownerId.equals(user.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权访问该提交记录");
        }
    }

    private AttachmentVO toVo(ReportAttachmentEntity entity) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(entity.getId());
        vo.setSubmissionId(entity.getSubmissionId());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setContentType(entity.getContentType());
        vo.setUploadedAt(entity.getUploadedAt());
        return vo;
    }

    public record DownloadData(String filename, String contentType, byte[] bytes) {
    }
}

