package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_device_config")
public class TaskDeviceConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long deviceId;
    private Integer maxQuantity;
    private LocalDateTime createdAt;
}
