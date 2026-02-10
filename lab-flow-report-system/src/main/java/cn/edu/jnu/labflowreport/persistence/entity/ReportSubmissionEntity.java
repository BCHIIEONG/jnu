package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("report_submission")
public class ReportSubmissionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private Integer versionNo;
    private String contentMd;
    private String submitStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}

