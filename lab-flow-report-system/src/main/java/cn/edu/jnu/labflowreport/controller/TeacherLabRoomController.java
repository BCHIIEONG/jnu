package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomVO;
import cn.edu.jnu.labflowreport.admin.service.LabRoomManagementService;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.auth.security.SecurityUtils;
import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/lab-rooms")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherLabRoomController {

    private final LabRoomManagementService labRoomManagementService;

    public TeacherLabRoomController(LabRoomManagementService labRoomManagementService) {
        this.labRoomManagementService = labRoomManagementService;
    }

    @GetMapping
    public ApiResponse<List<AdminLabRoomVO>> listLabRooms() {
        return ApiResponse.success(labRoomManagementService.listLabRooms());
    }

    @PostMapping
    public ApiResponse<AdminLabRoomVO> createLabRoom(@Valid @RequestBody AdminLabRoomRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("创建成功", labRoomManagementService.createLabRoom(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminLabRoomVO> updateLabRoom(@PathVariable Long id, @Valid @RequestBody AdminLabRoomRequest request) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        return ApiResponse.success("更新成功", labRoomManagementService.updateLabRoom(actor, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLabRoom(@PathVariable Long id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        labRoomManagementService.deleteLabRoom(actor, id);
        return ApiResponse.success("删除成功", null);
    }
}
