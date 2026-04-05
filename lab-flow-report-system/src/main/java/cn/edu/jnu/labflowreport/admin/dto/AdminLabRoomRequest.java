package cn.edu.jnu.labflowreport.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import java.util.List;

public record AdminLabRoomRequest(
        @NotBlank(message = "name 不能为空")
        String name,
        String location,
        @Valid
        List<AdminLabRoomOpenSlotRequest> openSlots
) {
}
