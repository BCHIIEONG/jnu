package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_prestudy")
public class TaskPrestudyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private Integer version;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
