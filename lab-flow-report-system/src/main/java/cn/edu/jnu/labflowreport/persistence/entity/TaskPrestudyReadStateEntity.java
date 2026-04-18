package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_prestudy_read_state")
public class TaskPrestudyReadStateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long prestudyId;
    private Long studentId;
    private Integer lastReadVersion;
    private LocalDateTime lastReadAt;
}
