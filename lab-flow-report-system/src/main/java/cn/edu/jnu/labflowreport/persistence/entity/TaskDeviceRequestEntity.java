package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("task_device_request")
public class TaskDeviceRequestEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private Long deviceId;
    private Integer quantity;
    private String status;
    private String note;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private Long checkoutBy;
    private LocalDateTime checkoutAt;
    private Long returnBy;
    private LocalDateTime returnAt;
    private LocalDateTime createdAt;
}
