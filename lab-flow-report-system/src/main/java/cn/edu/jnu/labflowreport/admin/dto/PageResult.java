package cn.edu.jnu.labflowreport.admin.dto;

import java.util.List;

public record PageResult<T>(
        int page,
        int size,
        long total,
        List<T> items
) {
}

