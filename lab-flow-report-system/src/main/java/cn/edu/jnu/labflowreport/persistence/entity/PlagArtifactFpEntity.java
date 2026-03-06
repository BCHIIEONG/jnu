package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("plag_artifact_fp")
public class PlagArtifactFpEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long taskId;
    private Long submissionId;
    private Long studentId;
    private Long attachmentId;
    private String artifactType;
    private String algo;
    private String fp64Hex;
    private Long byteLen;
    private String contentType;
    private String fileName;
    private LocalDateTime createdAt;
}

