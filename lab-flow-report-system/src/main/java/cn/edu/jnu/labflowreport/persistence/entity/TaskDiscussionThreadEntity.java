package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_discussion_thread")
public class TaskDiscussionThreadEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String type;
    private Long creatorId;
    private Long latestMessageId;
    private LocalDateTime latestMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
