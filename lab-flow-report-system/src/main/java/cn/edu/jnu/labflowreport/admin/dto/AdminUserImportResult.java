package cn.edu.jnu.labflowreport.admin.dto;

import java.util.List;

public record AdminUserImportResult(
        int totalRows,
        int created,
        int updated,
        int failed,
        List<AdminUserImportError> errors
) {
}

