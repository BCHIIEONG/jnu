package cn.edu.jnu.labflowreport.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("plag_task_run")
public class PlagTaskRunEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String status;
    private String algoVersion;
    private BigDecimal textThreshold;
    private BigDecimal imageThreshold;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String summaryJson;
}

