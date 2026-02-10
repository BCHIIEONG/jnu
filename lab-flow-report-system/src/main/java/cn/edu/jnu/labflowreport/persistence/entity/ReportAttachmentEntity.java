package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("report_attachment")
public class ReportAttachmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
}

