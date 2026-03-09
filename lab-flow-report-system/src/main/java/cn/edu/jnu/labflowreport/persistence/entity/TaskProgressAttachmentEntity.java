package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_progress_attachment")
public class TaskProgressAttachmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long progressLogId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String relativePath;
    private String fileSha256;
    private LocalDateTime createdAt;
}
