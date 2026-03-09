package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_attachment")
public class TaskAttachmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
