package cn.edu.jnu.labflowreport.auth.dto;

import java.util.List;

public record UserProfile(
        Long id,
        String username,
        String displayName,
        List<String> roles
) {
}

