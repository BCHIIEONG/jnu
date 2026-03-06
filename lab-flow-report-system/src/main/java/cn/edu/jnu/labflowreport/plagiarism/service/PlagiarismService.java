package cn.edu.jnu.labflowreport.plagiarism.service;

import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.config.PlagiarismProperties;
import cn.edu.jnu.labflowreport.persistence.entity.PlagArtifactFpEntity;
import cn.edu.jnu.labflowreport.persistence.entity.PlagSubmissionBestMatchEntity;
import cn.edu.jnu.labflowreport.persistence.entity.PlagTaskRunEntity;
import cn.edu.jnu.labflowreport.persistence.entity.ReportAttachmentEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagArtifactFpMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagSubmissionBestMatchMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.PlagTaskRunMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ExpTaskMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportAttachmentMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.ReportSubmissionMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismRunVO;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismSummaryVO;
import cn.edu.jnu.labflowreport.plagiarism.vo.PlagiarismStudentHistoryVO;
import cn.edu.jnu.labflowreport.storage.FileStorageService;
import cn.edu.jnu.labflowreport.workflow.vo.SubmissionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlagiarismService {

    private static final String ALGO_SIMHASH64 = "SIMHASH64";
    private static final String ALGO_DHASH64 = "DHASH64";
    private static final String ART_SUBMISSION_TEXT = "SUBMISSION_TEXT";
    private static final String ART_ATTACHMENT_TEXT = "ATTACHMENT_TEXT";
    private static final String ART_ATTACHMENT_IMAGE = "ATTACHMENT_IMAGE";

    private static final Set<String> TEXT_EXTS = Set.of(
            "txt", "md", "json", "xml", "yml", "yaml", "sql",
            "java", "py", "js", "ts", "c", "cpp", "h", "hpp", "cs", "go", "rs", "kt",
            "sh", "bat", "ps1"
    );

    private static final Set<String> SKIP_EXTS = Set.of(
            "xlsx", "xls", "doc", "docx", "pdf", "ppt", "pptx", "zip", "rar", "7z"
    );

    private final PlagiarismProperties props;
    private final ObjectMapper objectMapper;
    private final FileStorageService storageService;
    private final ReportSubmissionMapper submissionMapper;
    private final ReportAttachmentMapper attachmentMapper;
    private final SysUserMapper sysUserMapper;
    private final ExpTaskMapper expTaskMapper;
    private final PlagTaskRunMapper runMapper;
    private final PlagArtifactFpMapper fpMapper;
    private final PlagSubmissionBestMatchMapper bestMapper;

    public PlagiarismService(
            PlagiarismProperties props,
            ObjectMapper objectMapper,
            FileStorageService storageService,
            ReportSubmissionMapper submissionMapper,
            ReportAttachmentMapper attachmentMapper,
            SysUserMapper sysUserMapper,
            ExpTaskMapper expTaskMapper,
            PlagTaskRunMapper runMapper,
            PlagArtifactFpMapper fpMapper,
            PlagSubmissionBestMatchMapper bestMapper
    ) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.submissionMapper = submissionMapper;
        this.attachmentMapper = attachmentMapper;
        this.sysUserMapper = sysUserMapper;
        this.expTaskMapper = expTaskMapper;
        this.runMapper = runMapper;
        this.fpMapper = fpMapper;
        this.bestMapper = bestMapper;
    }

    @Transactional
    public PlagiarismRunVO runForTask(AuthenticatedUser actor, Long taskId) {
        ensureTeacher(actor);
        if (taskId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "taskId 不能为空");
        }
        ensureTeacherCanManageTask(actor, taskId);

        List<SubmissionVO> all = submissionMapper.findSubmissionsByTask(taskId);
        List<SubmissionVO> submissions = all.stream()
                .filter(s -> s != null && s.getId() != null && s.getStudentId() != null)
                .toList();
        Set<Long> studentsDistinct = submissions.stream().map(SubmissionVO::getStudentId).collect(Collectors.toSet());
        if (studentsDistinct.size() < 2) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "提交数量不足，至少需要 2 位不同学生的报告才能查重");
        }

        PlagTaskRunEntity run = new PlagTaskRunEntity();
        run.setTaskId(taskId);
        run.setStatus("RUNNING");
        run.setAlgoVersion("v1");
        run.setTextThreshold(BigDecimal.valueOf(props.textThreshold()).setScale(4, RoundingMode.HALF_UP));
        run.setImageThreshold(BigDecimal.valueOf(props.imageThreshold()).setScale(4, RoundingMode.HALF_UP));
        run.setStartedAt(LocalDateTime.now());
        runMapper.insert(run);

        List<Long> submissionIds = submissions.stream().map(SubmissionVO::getId).toList();
        Map<Long, List<ReportAttachmentEntity>> attachmentsBySubmission = new HashMap<>();
        for (Long sid : submissionIds) {
            attachmentsBySubmission.put(sid, attachmentMapper.findBySubmissionId(sid));
        }

        Map<Long, List<Artifact>> artifactsBySubmission = new HashMap<>();
        Map<Long, List<PlagiarismSummaryVO.SkippedAttachment>> skippedBySubmission = new HashMap<>();

        int totalArtifacts = 0;
        int totalSkipped = 0;

        for (SubmissionVO s : submissions) {
            List<Artifact> artifacts = new ArrayList<>();
            List<PlagiarismSummaryVO.SkippedAttachment> skipped = new ArrayList<>();

            String plain = MarkdownText.toPlainText(Objects.toString(s.getContentMd(), ""));
            long fpText = SimHash64.fingerprint(plain);
            artifacts.add(Artifact.submissionText(fpText));
            fpMapper.insert(toFpEntity(run.getId(), taskId, s, null, ART_SUBMISSION_TEXT, ALGO_SIMHASH64, fpText, plain.length(), "text/plain", "submission.md"));
            totalArtifacts++;

            List<ReportAttachmentEntity> atts = attachmentsBySubmission.getOrDefault(s.getId(), List.of());
            int count = 0;
            for (ReportAttachmentEntity att : atts) {
                if (count >= props.maxAttachmentsPerSubmission()) {
                    skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "超过附件数量上限"));
                    totalSkipped++;
                    continue;
                }
                count++;

                AttachmentType type = classify(att);
                if (type == AttachmentType.SKIP) {
                    skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "已跳过类型"));
                    totalSkipped++;
                    continue;
                }

                byte[] bytes;
                try {
                    bytes = storageService.readBytes(att.getFilePath());
                } catch (Exception e) {
                    skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "附件读取失败"));
                    totalSkipped++;
                    continue;
                }

                if (type == AttachmentType.TEXT) {
                    if (bytes.length > props.maxTextAttachmentBytes()) {
                        skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "文本附件过大"));
                        totalSkipped++;
                        continue;
                    }
                    String text = new String(bytes, StandardCharsets.UTF_8);
                    long fp = SimHash64.fingerprint(text);
                    artifacts.add(Artifact.attachmentText(att.getId(), att.getFileName(), fp));
                    fpMapper.insert(toFpEntity(run.getId(), taskId, s, att, ART_ATTACHMENT_TEXT, ALGO_SIMHASH64, fp, bytes.length, att.getContentType(), att.getFileName()));
                    totalArtifacts++;
                } else if (type == AttachmentType.IMAGE) {
                    if (bytes.length > props.maxImageBytes()) {
                        skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "图片附件过大"));
                        totalSkipped++;
                        continue;
                    }
                    try {
                        long fp = DHash64.fingerprint(bytes);
                        artifacts.add(Artifact.attachmentImage(att.getId(), att.getFileName(), fp));
                        fpMapper.insert(toFpEntity(run.getId(), taskId, s, att, ART_ATTACHMENT_IMAGE, ALGO_DHASH64, fp, bytes.length, att.getContentType(), att.getFileName()));
                        totalArtifacts++;
                    } catch (Exception ex) {
                        skipped.add(new PlagiarismSummaryVO.SkippedAttachment(att.getFileName(), att.getContentType(), "图片解析失败"));
                        totalSkipped++;
                    }
                }
            }

            artifactsBySubmission.put(s.getId(), artifacts);
            skippedBySubmission.put(s.getId(), skipped);
        }

        // Pre-compute plain text for sentence fragments (submission text only).
        Map<Long, String> submissionPlain = new HashMap<>();
        for (SubmissionVO s : submissions) {
            submissionPlain.put(s.getId(), MarkdownText.toPlainText(Objects.toString(s.getContentMd(), "")));
        }

        for (SubmissionVO s : submissions) {
            Best best = findBest(s, submissions, artifactsBySubmission);
            List<PlagiarismSummaryVO.EvidenceItem> evidence = new ArrayList<>();

            // Submission text evidence (for highlighting).
            if (best.bestOtherSubmissionId != null) {
                long aText = getSubmissionTextFp(artifactsBySubmission.get(s.getId()));
                long bText = getSubmissionTextFp(artifactsBySubmission.get(best.bestOtherSubmissionId));
                double simText = SimHash64.similarity(aText, bText);
                List<SentenceSimilarity.Fragment> frags = SentenceSimilarity.topFragments(
                        submissionPlain.getOrDefault(s.getId(), ""),
                        submissionPlain.getOrDefault(best.bestOtherSubmissionId, ""),
                        10,
                        0.85
                );
                if (!frags.isEmpty()) {
                    Map<String, Object> detail = new HashMap<>();
                    List<Map<String, Object>> fragments = new ArrayList<>();
                    for (SentenceSimilarity.Fragment f : frags) {
                        fragments.add(Map.of(
                                "text", f.text(),
                                "score", BigDecimal.valueOf(f.score()).setScale(4, RoundingMode.HALF_UP)
                        ));
                    }
                    detail.put("fragments", fragments);
                    evidence.add(new PlagiarismSummaryVO.EvidenceItem(
                            ART_SUBMISSION_TEXT,
                            BigDecimal.valueOf(simText).setScale(4, RoundingMode.HALF_UP),
                            detail
                    ));
                }
            }

            // Add best non-text evidence (attachments/images).
            evidence.addAll(best.evidence);

            // Keep top K.
            evidence.sort((x, y) -> y.score().compareTo(x.score()));
            if (evidence.size() > 5) {
                evidence = evidence.subList(0, 5);
            }

            PlagSubmissionBestMatchEntity bm = new PlagSubmissionBestMatchEntity();
            bm.setRunId(run.getId());
            bm.setTaskId(taskId);
            bm.setSubmissionId(s.getId());
            bm.setStudentId(s.getStudentId());
            bm.setBestOtherSubmissionId(best.bestOtherSubmissionId);
            bm.setBestOtherStudentId(best.bestOtherStudentId);
            bm.setMaxScore(BigDecimal.valueOf(best.maxScore).setScale(4, RoundingMode.HALF_UP));
            bm.setCreatedAt(LocalDateTime.now());
            bm.setEvidenceJson(writeJsonSafe(evidence));
            bm.setSkippedAttachmentsJson(writeJsonSafe(skippedBySubmission.getOrDefault(s.getId(), List.of())));
            bestMapper.insert(bm);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("submissionsTotal", submissions.size());
        summary.put("studentsDistinct", studentsDistinct.size());
        summary.put("mode", "ALL_VERSIONS");
        summary.put("artifacts", totalArtifacts);
        summary.put("skippedAttachments", totalSkipped);

        run.setStatus("DONE");
        run.setFinishedAt(LocalDateTime.now());
        run.setSummaryJson(writeJsonSafe(summary));
        runMapper.updateById(run);

        return new PlagiarismRunVO(run.getId(), run.getStatus(), run.getStartedAt(), run.getFinishedAt());
    }

    public PlagiarismSummaryVO getSummary(AuthenticatedUser actor, Long submissionId) {
        ensureTeacher(actor);
        if (submissionId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "submissionId 不能为空");
        }
        SubmissionVO submission = submissionMapper.findSubmissionById(submissionId);
        if (submission == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }
        Long taskId = submission.getTaskId();
        ensureTeacherCanManageTask(actor, taskId);
        PlagTaskRunEntity run = runMapper.findLatestByTaskId(taskId);
        if (run == null) {
            return new PlagiarismSummaryVO(null, taskId, BigDecimal.ZERO, null, 0, 0, 0, 0, List.of(), List.of());
        }
        PlagSubmissionBestMatchEntity bm = bestMapper.findByRunIdAndSubmissionId(run.getId(), submissionId);
        if (bm == null) {
            return new PlagiarismSummaryVO(run.getId(), taskId, BigDecimal.ZERO, null, 0, 0, 0, 0, List.of(), List.of());
        }

        PlagiarismSummaryVO.TopMatchStudent top = null;
        if (bm.getBestOtherStudentId() != null) {
            SysUserEntity u = sysUserMapper.selectById(bm.getBestOtherStudentId());
            if (u != null) {
                top = new PlagiarismSummaryVO.TopMatchStudent(u.getId(), u.getUsername(), u.getDisplayName());
            }
        }

        List<PlagiarismSummaryVO.EvidenceItem> evidence = readEvidenceList(bm.getEvidenceJson());
        List<PlagiarismSummaryVO.SkippedAttachment> skipped = readSkippedList(bm.getSkippedAttachmentsJson());

        int imagesProcessed = 0;
        int textAttProcessed = 0;
        for (var c : fpMapper.countByRunIdAndSubmissionId(run.getId(), submissionId)) {
            String t = c == null ? null : c.getArtifactType();
            long n = c == null || c.getCnt() == null ? 0 : c.getCnt();
            if (ART_ATTACHMENT_IMAGE.equalsIgnoreCase(t)) imagesProcessed += (int) n;
            if (ART_ATTACHMENT_TEXT.equalsIgnoreCase(t)) textAttProcessed += (int) n;
        }

        int imagesSkipped = 0;
        int textAttSkipped = 0;
        for (var s : skipped) {
            AttachmentType t = classify(s.fileName(), s.contentType());
            if (t == AttachmentType.IMAGE) imagesSkipped++;
            if (t == AttachmentType.TEXT) textAttSkipped++;
        }

        return new PlagiarismSummaryVO(
                run.getId(),
                taskId,
                bm.getMaxScore() == null ? BigDecimal.ZERO : bm.getMaxScore(),
                top,
                imagesProcessed,
                imagesSkipped,
                textAttProcessed,
                textAttSkipped,
                evidence,
                skipped
        );
    }

    public PlagiarismStudentHistoryVO getStudentHistory(AuthenticatedUser actor, Long submissionId) {
        ensureTeacher(actor);
        if (submissionId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "submissionId 不能为空");
        }

        SubmissionVO current = submissionMapper.findSubmissionById(submissionId);
        if (current == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "提交记录不存在");
        }
        Long taskId = current.getTaskId();
        Long studentId = current.getStudentId();
        Integer currentVersionNo = current.getVersionNo();

        ensureTeacherCanManageTask(actor, taskId);

        SysUserEntity me = sysUserMapper.selectById(studentId);
        PlagiarismStudentHistoryVO.StudentInfo studentInfo = me == null
                ? new PlagiarismStudentHistoryVO.StudentInfo(studentId, null, null)
                : new PlagiarismStudentHistoryVO.StudentInfo(me.getId(), me.getUsername(), me.getDisplayName());

        // Always return all versions for the student. If there's no run yet, hasResult will be false for all rows.
        List<SubmissionVO> my = submissionMapper.findMySubmissionsByTask(taskId, studentId);
        my = my == null ? List.of() : my.stream().filter(Objects::nonNull).toList();
        my = my.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getVersionNo() == null ? 0 : a.getVersionNo(),
                        b.getVersionNo() == null ? 0 : b.getVersionNo()
                ))
                .toList();

        PlagTaskRunEntity run = runMapper.findLatestByTaskId(taskId);
        Long runId = run == null ? null : run.getId();

        Map<Long, PlagSubmissionBestMatchEntity> bmBySubmission = new HashMap<>();
        Map<Long, PlagiarismStudentHistoryVO.TopMatchStudent> topByOtherStudent = new HashMap<>();

        if (runId != null && studentId != null) {
            List<PlagSubmissionBestMatchEntity> best = bestMapper.selectList(
                    new LambdaQueryWrapper<PlagSubmissionBestMatchEntity>()
                            .eq(PlagSubmissionBestMatchEntity::getRunId, runId)
                            .eq(PlagSubmissionBestMatchEntity::getTaskId, taskId)
                            .eq(PlagSubmissionBestMatchEntity::getStudentId, studentId)
            );
            if (best != null) {
                for (var bm : best) {
                    if (bm == null || bm.getSubmissionId() == null) continue;
                    bmBySubmission.put(bm.getSubmissionId(), bm);
                }
            }

            Set<Long> otherIds = bmBySubmission.values().stream()
                    .map(PlagSubmissionBestMatchEntity::getBestOtherStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!otherIds.isEmpty()) {
                for (var u : sysUserMapper.selectBatchIds(otherIds)) {
                    if (u == null || u.getId() == null) continue;
                    topByOtherStudent.put(u.getId(), new PlagiarismStudentHistoryVO.TopMatchStudent(u.getId(), u.getUsername(), u.getDisplayName()));
                }
            }
        }

        List<PlagiarismStudentHistoryVO.VersionRisk> versions = new ArrayList<>();
        BigDecimal maxAcross = BigDecimal.ZERO;
        BigDecimal maxEarlier = BigDecimal.ZERO;

        for (var s : my) {
            Long sid = s.getId();
            Integer vno = s.getVersionNo();
            PlagSubmissionBestMatchEntity bm = sid == null ? null : bmBySubmission.get(sid);
            boolean has = bm != null;
            BigDecimal score = has && bm.getMaxScore() != null ? bm.getMaxScore() : BigDecimal.ZERO;
            PlagiarismStudentHistoryVO.TopMatchStudent top = null;
            if (has && bm.getBestOtherStudentId() != null) {
                top = topByOtherStudent.get(bm.getBestOtherStudentId());
                // If user record is missing, still return id so UI can show something stable.
                if (top == null) {
                    top = new PlagiarismStudentHistoryVO.TopMatchStudent(bm.getBestOtherStudentId(), null, null);
                }
            }

            versions.add(new PlagiarismStudentHistoryVO.VersionRisk(
                    sid,
                    vno,
                    s.getSubmittedAt(),
                    score,
                    top,
                    has
            ));

            if (score.compareTo(maxAcross) > 0) {
                maxAcross = score;
            }
            if (currentVersionNo != null && vno != null && vno < currentVersionNo) {
                if (score.compareTo(maxEarlier) > 0) {
                    maxEarlier = score;
                }
            }
        }

        return new PlagiarismStudentHistoryVO(
                runId,
                taskId,
                studentInfo,
                currentVersionNo,
                maxAcross,
                maxEarlier,
                versions
        );
    }

    private void ensureTeacher(AuthenticatedUser actor) {
        if (actor == null || actor.roleCodes() == null) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        if (!actor.roleCodes().contains("ROLE_TEACHER") && !actor.roleCodes().contains("ROLE_ADMIN")) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "无权限访问该资源");
        }
    }

    private void ensureTeacherCanManageTask(AuthenticatedUser actor, Long taskId) {
        if (actor == null || actor.roleCodes() == null || taskId == null) {
            return;
        }
        if (actor.roleCodes().contains("ROLE_ADMIN")) {
            return;
        }
        // teacher: only own tasks
        Long publisherId = expTaskMapper.findPublisherId(taskId);
        if (publisherId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "任务不存在");
        }
        if (!publisherId.equals(actor.userId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, HttpStatus.FORBIDDEN, "只能对自己发布的任务运行/查看查重结果");
        }
    }

    // Latest-by-student mode was removed; plagiarism now runs across all versions.

    private PlagArtifactFpEntity toFpEntity(
            Long runId,
            Long taskId,
            SubmissionVO submission,
            ReportAttachmentEntity att,
            String artifactType,
            String algo,
            long fp,
            long byteLen,
            String contentType,
            String fileName
    ) {
        PlagArtifactFpEntity e = new PlagArtifactFpEntity();
        e.setRunId(runId);
        e.setTaskId(taskId);
        e.setSubmissionId(submission.getId());
        e.setStudentId(submission.getStudentId());
        e.setAttachmentId(att == null ? null : att.getId());
        e.setArtifactType(artifactType);
        e.setAlgo(algo);
        e.setFp64Hex(toHex64(fp));
        e.setByteLen(byteLen);
        e.setContentType(contentType);
        e.setFileName(fileName);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private String toHex64(long v) {
        return String.format(Locale.ROOT, "%016x", v);
    }

    private AttachmentType classify(ReportAttachmentEntity att) {
        String name = Objects.toString(att.getFileName(), "");
        String ct = Objects.toString(att.getContentType(), "");
        return classify(name, ct);
    }

    private AttachmentType classify(String fileName, String contentType) {
        String name = Objects.toString(fileName, "");
        String ct = Objects.toString(contentType, "");
        String ext = extLower(name);

        if (!ext.isBlank() && SKIP_EXTS.contains(ext)) {
            return AttachmentType.SKIP;
        }
        if (ct.toLowerCase(Locale.ROOT).contains("spreadsheet") || ext.equals("xlsx") || ext.equals("xls")) {
            return AttachmentType.SKIP;
        }
        if (ct.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return AttachmentType.IMAGE;
        }
        if (ct.toLowerCase(Locale.ROOT).startsWith("text/")) {
            return AttachmentType.TEXT;
        }
        if (!ext.isBlank() && TEXT_EXTS.contains(ext)) {
            return AttachmentType.TEXT;
        }
        if (!ext.isBlank() && (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg"))) {
            return AttachmentType.IMAGE;
        }
        return AttachmentType.SKIP;
    }

    private String extLower(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) return "";
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private Best findBest(SubmissionVO self, Iterable<SubmissionVO> all, Map<Long, List<Artifact>> artifactsBySubmission) {
        Best best = new Best();
        best.maxScore = 0.0;
        for (SubmissionVO other : all) {
            if (other.getId() == null || self.getId() == null) continue;
            if (Objects.equals(other.getId(), self.getId())) continue;
            if (self.getStudentId() != null && other.getStudentId() != null && Objects.equals(self.getStudentId(), other.getStudentId())) {
                continue; // do not match against the same student's other versions
            }
            double max = 0.0;
            List<PlagiarismSummaryVO.EvidenceItem> ev = new ArrayList<>();

            ArtifactPair bestText = bestPair(artifactsBySubmission.get(self.getId()), artifactsBySubmission.get(other.getId()), ALGO_SIMHASH64);
            if (bestText != null) {
                max = Math.max(max, bestText.score);
                if (bestText.a != null && bestText.a.type.equals(ART_ATTACHMENT_TEXT) && bestText.a.attachmentId != null) {
                    ev.add(new PlagiarismSummaryVO.EvidenceItem(
                            ART_ATTACHMENT_TEXT,
                            BigDecimal.valueOf(bestText.score).setScale(4, RoundingMode.HALF_UP),
                            Map.of("attachmentId", bestText.a.attachmentId, "fileName", Objects.toString(bestText.a.fileName, ""))
                    ));
                }
            }

            ArtifactPair bestImg = bestPair(artifactsBySubmission.get(self.getId()), artifactsBySubmission.get(other.getId()), ALGO_DHASH64);
            if (bestImg != null) {
                max = Math.max(max, bestImg.score);
                if (bestImg.a != null && bestImg.b != null && bestImg.a.attachmentId != null && bestImg.b.attachmentId != null) {
                    ev.add(new PlagiarismSummaryVO.EvidenceItem(
                            ART_ATTACHMENT_IMAGE,
                            BigDecimal.valueOf(bestImg.score).setScale(4, RoundingMode.HALF_UP),
                            Map.of(
                                    "attachmentIdA", bestImg.a.attachmentId,
                                    "attachmentIdB", bestImg.b.attachmentId,
                                    "fileNameA", Objects.toString(bestImg.a.fileName, ""),
                                    "fileNameB", Objects.toString(bestImg.b.fileName, ""),
                                    "similarity", BigDecimal.valueOf(bestImg.score).setScale(4, RoundingMode.HALF_UP)
                            )
                    ));
                }
            }

            if (max > best.maxScore) {
                best.maxScore = max;
                best.bestOtherSubmissionId = other.getId();
                best.bestOtherStudentId = other.getStudentId();
                best.evidence = ev;
            }
        }
        return best;
    }

    private ArtifactPair bestPair(List<Artifact> a, List<Artifact> b, String algo) {
        if (a == null || b == null) return null;
        double best = 0.0;
        Artifact bestA = null;
        Artifact bestB = null;
        for (Artifact x : a) {
            if (!algo.equals(x.algo)) continue;
            for (Artifact y : b) {
                if (!algo.equals(y.algo)) continue;
                double score = algo.equals(ALGO_DHASH64)
                        ? DHash64.similarity(x.fp, y.fp)
                        : SimHash64.similarity(x.fp, y.fp);
                if (score > best) {
                    best = score;
                    bestA = x;
                    bestB = y;
                }
            }
        }
        if (bestA == null || bestB == null) return null;
        return new ArtifactPair(bestA, bestB, best);
    }

    private long getSubmissionTextFp(List<Artifact> artifacts) {
        if (artifacts == null) return 0L;
        for (Artifact a : artifacts) {
            if (ART_SUBMISSION_TEXT.equals(a.type) && ALGO_SIMHASH64.equals(a.algo)) {
                return a.fp;
            }
        }
        return 0L;
    }

    private String writeJsonSafe(Object o) {
        if (o == null) return "[]";
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<PlagiarismSummaryVO.EvidenceItem> readEvidenceList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PlagiarismSummaryVO.EvidenceItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<PlagiarismSummaryVO.SkippedAttachment> readSkippedList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PlagiarismSummaryVO.SkippedAttachment>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private enum AttachmentType {TEXT, IMAGE, SKIP}

    private record Artifact(String type, String algo, long fp, Long attachmentId, String fileName) {
        static Artifact submissionText(long fp) {
            return new Artifact(ART_SUBMISSION_TEXT, ALGO_SIMHASH64, fp, null, "submission.md");
        }

        static Artifact attachmentText(Long id, String name, long fp) {
            return new Artifact(ART_ATTACHMENT_TEXT, ALGO_SIMHASH64, fp, id, name);
        }

        static Artifact attachmentImage(Long id, String name, long fp) {
            return new Artifact(ART_ATTACHMENT_IMAGE, ALGO_DHASH64, fp, id, name);
        }
    }

    private static final class Best {
        Long bestOtherSubmissionId;
        Long bestOtherStudentId;
        double maxScore;
        List<PlagiarismSummaryVO.EvidenceItem> evidence = List.of();
    }

    private record ArtifactPair(Artifact a, Artifact b, double score) {
    }
}
