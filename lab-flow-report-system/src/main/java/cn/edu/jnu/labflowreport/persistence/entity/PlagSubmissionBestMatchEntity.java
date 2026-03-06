package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("plag_submission_best_match")
public class PlagSubmissionBestMatchEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long runId;
    private Long taskId;
    private Long submissionId;
    private Long studentId;
    private Long bestOtherSubmissionId;
    private Long bestOtherStudentId;
    private java.math.BigDecimal maxScore;
    private String evidenceJson;
    private String skippedAttachmentsJson;
    private LocalDateTime createdAt;
}

