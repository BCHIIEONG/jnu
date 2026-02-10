package cn.edu.jnu.labflowreport.workflow.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ScoreExportRowVO {
    private String studentUsername;
    private String studentDisplayName;
    private Integer versionNo;
    private BigDecimal score;
    private String comment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}

