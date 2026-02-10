package cn.edu.jnu.labflowreport.auth.model;

import java.util.List;

public record TokenPayload(
        Long userId,
        String username,
        List<String> roleCodes
) {
}

