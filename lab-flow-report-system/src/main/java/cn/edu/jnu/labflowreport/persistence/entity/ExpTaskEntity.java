package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("exp_task")
public class ExpTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Long publisherId;
    private LocalDateTime deadlineAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

