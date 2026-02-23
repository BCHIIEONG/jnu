package cn.edu.jnu.labflowreport.admin.dto;

public record AdminUserImportError(
        int rowNo,
        String username,
        String message
) {
}

