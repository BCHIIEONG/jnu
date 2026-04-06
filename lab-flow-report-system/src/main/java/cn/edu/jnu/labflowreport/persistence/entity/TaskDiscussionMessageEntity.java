package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_discussion_message")
public class TaskDiscussionMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long threadId;
    private Long authorId;
    private String authorRole;
    private String content;
    private LocalDateTime createdAt;
}
