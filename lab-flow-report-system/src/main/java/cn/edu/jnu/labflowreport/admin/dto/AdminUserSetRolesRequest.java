package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AdminUserSetRolesRequest(
        @NotEmpty(message = "roleCodes 不能为空")
        List<String> roleCodes
) {
}

