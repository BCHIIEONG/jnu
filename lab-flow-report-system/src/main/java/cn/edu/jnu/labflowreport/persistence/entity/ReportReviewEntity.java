package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("report_review")
public class ReportReviewEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long teacherId;
    private BigDecimal score;
    private String comment;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;
}

