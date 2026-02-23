package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AdminSemesterRequest(
        @NotBlank(message = "name 不能为空")
        String name,
        LocalDate startDate,
        LocalDate endDate
) {
}

