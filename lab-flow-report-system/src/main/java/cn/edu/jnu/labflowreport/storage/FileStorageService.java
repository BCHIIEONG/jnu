package cn.edu.jnu.labflowreport.storage;

import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(StorageProperties props) {
        String configured = props == null ? null : props.dir();
        if (configured == null || configured.isBlank()) {
            configured = "uploads";
        }
        this.baseDir = Path.of(configured).toAbsolutePath().normalize();
    }

    public String saveReportAttachment(Long submissionId, MultipartFile file) {
        if (submissionId == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "submissionId 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "请选择要上传的文件");
        }

        String originalName = Objects.toString(file.getOriginalFilename(), "attachment");
        String safeName = sanitizeFilename(originalName);
        String ext = extractExt(safeName);

        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relative = Path.of("report-attachments", String.valueOf(submissionId), storedName).toString();
        Path target = resolveUnderBase(relative);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
        return relative.replace('\\', '/');
    }

    public byte[] readBytes(String relativePath) {
        Path target = resolveUnderBase(relativePath);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "附件文件不存在或无法读取");
        }
    }

    private Path resolveUnderBase(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "filePath 不能为空");
        }
        Path p = baseDir.resolve(relativePath).normalize();
        if (!p.startsWith(baseDir)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "非法文件路径");
        }
        return p;
    }

    private String sanitizeFilename(String name) {
        // Keep it simple for Windows: remove path separators and control chars.
        return name.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }

    private String extractExt(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx <= 0 || idx == filename.length() - 1) {
            return "";
        }
        String ext = filename.substring(idx);
        if (ext.length() > 12) {
            return "";
        }
        return ext;
    }
}
