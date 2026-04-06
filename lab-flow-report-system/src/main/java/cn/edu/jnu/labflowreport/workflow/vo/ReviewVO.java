package cn.edu.jnu.labflowreport.workflow.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ReviewVO {
    private Long id;
    private Long submissionId;
    private Long teacherId;
    private String teacherUsername;
    private String teacherDisplayName;
    private BigDecimal score;
    private String comment;
    private LocalDateTime reviewedAt;
    private List<String> issueTags;
}
