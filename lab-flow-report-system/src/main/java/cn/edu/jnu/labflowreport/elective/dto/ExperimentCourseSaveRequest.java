package cn.edu.jnu.labflowreport.elective.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExperimentCourseSaveRequest(
        @NotBlank(message = "title 不能为空")
        String title,
        String description,
        @NotNull(message = "semesterId 不能为空")
        Long semesterId,
        @NotNull(message = "enrollDeadlineAt 不能为空")
        @Future(message = "选课截止时间必须晚于当前时间")
        LocalDateTime enrollDeadlineAt,
        List<Long> targetClassIds,
        List<Long> targetStudentIds,
        @NotEmpty(message = "至少配置一个可选场次")
        List<@Valid ExperimentCourseSlotRequest> slots
) {
    public record ExperimentCourseSlotRequest(
            Long id,
            String name,
            @NotBlank(message = "mode 不能为空")
            String mode,
            @NotNull(message = "firstLessonDate 不能为空")
            LocalDate firstLessonDate,
            @NotNull(message = "slotId 不能为空")
            Long slotId,
            @NotNull(message = "labRoomId 不能为空")
            Long labRoomId,
            @NotNull(message = "capacity 不能为空")
            @Min(value = 1, message = "capacity 至少为 1")
            Integer capacity,
            String repeatPattern,
            String rangeMode,
            LocalDate rangeStartDate,
            LocalDate rangeEndDate
    ) {
    }
}
