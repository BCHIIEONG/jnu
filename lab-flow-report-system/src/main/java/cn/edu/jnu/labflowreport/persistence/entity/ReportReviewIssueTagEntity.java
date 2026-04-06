package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("report_review_issue_tag")
public class ReportReviewIssueTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewId;
    private String tagCode;
    private LocalDateTime createdAt;
}
