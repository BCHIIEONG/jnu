package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_prestudy_attachment")
public class TaskPrestudyAttachmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prestudyId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
}
