package cn.edu.jnu.labflowreport.workflow.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReviewCreateRequest(
        @NotNull(message = "score 不能为空")
        @DecimalMin(value = "0.0", message = "score 不能小于 0")
        @DecimalMax(value = "100.0", message = "score 不能大于 100")
        BigDecimal score,
        String comment
) {
}

