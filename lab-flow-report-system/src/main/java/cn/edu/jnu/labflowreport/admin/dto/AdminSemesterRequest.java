package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AdminSemesterRequest(
        @NotBlank(message = "name 不能为空")
        String name,
        @NotNull(message = "startDate 不能为空")
        LocalDate startDate,
        @NotNull(message = "endDate 不能为空")
        LocalDate endDate
) {
}
