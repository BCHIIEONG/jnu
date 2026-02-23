package cn.edu.jnu.labflowreport.admin.dto;

public record AdminRoleVO(
        Long id,
        String code,
        String name,
        long userCount
) {
}

