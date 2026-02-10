package cn.edu.jnu.labflowreport.workflow.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SubmissionVO {
    private Long id;
    private Long taskId;
    private Long studentId;
    private String studentUsername;
    private String studentDisplayName;
    private Integer versionNo;
    private String contentMd;
    private String submitStatus;
    private LocalDateTime submittedAt;
}

