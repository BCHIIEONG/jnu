package cn.edu.jnu.labflowreport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.edu.jnu.labflowreport.admin.dto.AdminAuditLogVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminDeviceRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminDeviceVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminRoleVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminSemesterRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminSemesterVO;
import cn.edu.jnu.labflowreport.admin.dto.PageResult;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.export.ExcelExportService;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.AuditLogEntity;
import cn.edu.jnu.labflowreport.persistence.entity.DeviceEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysRoleEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserRoleEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.AuditLogMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.DeviceMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysRoleMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserRoleMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminResourceService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final LabRoomManagementService labRoomManagementService;
    private final DeviceMapper deviceMapper;
    private final SemesterMapper semesterMapper;
    private final AuditLogMapper auditLogMapper;
    private final AdminAuditService adminAuditService;
    private final ExcelExportService excelExportService;

    public AdminResourceService(
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            LabRoomManagementService labRoomManagementService,
            DeviceMapper deviceMapper,
            SemesterMapper semesterMapper,
            AuditLogMapper auditLogMapper,
            AdminAuditService adminAuditService,
            ExcelExportService excelExportService
    ) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.labRoomManagementService = labRoomManagementService;
        this.deviceMapper = deviceMapper;
        this.semesterMapper = semesterMapper;
        this.auditLogMapper = auditLogMapper;
        this.adminAuditService = adminAuditService;
        this.excelExportService = excelExportService;
    }

    public List<AdminRoleVO> listRoles() {
        List<SysRoleEntity> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>().orderByAsc(SysRoleEntity::getId));
        return roles.stream()
                .map(r -> new AdminRoleVO(
                        r.getId(),
                        r.getCode(),
                        r.getName(),
                        sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleEntity>().eq(SysUserRoleEntity::getRoleId, r.getId()))
                ))
                .toList();
    }

    public List<AdminLabRoomVO> listLabRooms() {
        return labRoomManagementService.listLabRooms();
    }

    @Transactional
    public AdminLabRoomVO createLabRoom(AuthenticatedUser actor, AdminLabRoomRequest request) {
        return labRoomManagementService.createLabRoom(actor, request);
    }

    @Transactional
    public AdminLabRoomVO updateLabRoom(AuthenticatedUser actor, Long id, AdminLabRoomRequest request) {
        return labRoomManagementService.updateLabRoom(actor, id, request);
    }

    @Transactional
    public void deleteLabRoom(AuthenticatedUser actor, Long id) {
        labRoomManagementService.deleteLabRoom(actor, id);
    }

    public String exportLabRoomsCsv(AuthenticatedUser actor) {
        return labRoomManagementService.exportLabRoomsCsv(actor);
    }

    public byte[] exportLabRoomsExcel(AuthenticatedUser actor) {
        return labRoomManagementService.exportLabRoomsExcel(actor);
    }

    public List<AdminDeviceVO> listDevices(String q, String status) {
        LambdaQueryWrapper<DeviceEntity> w = new LambdaQueryWrapper<DeviceEntity>().orderByAsc(DeviceEntity::getId);
        if (StringUtils.hasText(q)) {
            w.and(x -> x.like(DeviceEntity::getCode, q).or().like(DeviceEntity::getName, q));
        }
        if (StringUtils.hasText(status)) {
            w.eq(DeviceEntity::getStatus, status);
        }
        return deviceMapper.selectList(w).stream()
                .map(d -> new AdminDeviceVO(d.getId(), d.getCode(), d.getName(), d.getTotalQuantity(), d.getStatus(), d.getLocation(), d.getDescription(), d.getCreatedAt(), d.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public AdminDeviceVO createDevice(AuthenticatedUser actor, AdminDeviceRequest request) {
        DeviceEntity dup = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getCode, request.code().trim())
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "设备编码已存在");
        }
        DeviceEntity entity = new DeviceEntity();
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setTotalQuantity(request.totalQuantity());
        entity.setStatus(StringUtils.hasText(request.status()) ? request.status().trim() : "AVAILABLE");
        entity.setLocation(request.location());
        entity.setDescription(request.description());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        deviceMapper.insert(entity);
        adminAuditService.record(actor, AdminAuditActions.DEVICE_CREATE, "device", entity.getId(), Map.of("code", entity.getCode(), "name", entity.getName()));
        return new AdminDeviceVO(entity.getId(), entity.getCode(), entity.getName(), entity.getTotalQuantity(), entity.getStatus(), entity.getLocation(), entity.getDescription(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Transactional
    public AdminDeviceVO updateDevice(AuthenticatedUser actor, Long id, AdminDeviceRequest request) {
        DeviceEntity existing = deviceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备不存在");
        }
        DeviceEntity dup = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getCode, request.code().trim())
                .ne(DeviceEntity::getId, id)
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "设备编码已被占用");
        }
        deviceMapper.update(null, new LambdaUpdateWrapper<DeviceEntity>()
                .eq(DeviceEntity::getId, id)
                .set(DeviceEntity::getCode, request.code().trim())
                .set(DeviceEntity::getName, request.name().trim())
                .set(DeviceEntity::getTotalQuantity, request.totalQuantity())
                .set(DeviceEntity::getStatus, StringUtils.hasText(request.status()) ? request.status().trim() : "AVAILABLE")
                .set(DeviceEntity::getLocation, request.location())
                .set(DeviceEntity::getDescription, request.description())
                .set(DeviceEntity::getUpdatedAt, LocalDateTime.now()));
        adminAuditService.record(actor, AdminAuditActions.DEVICE_UPDATE, "device", id, Map.of("code", request.code().trim()));
        DeviceEntity updated = deviceMapper.selectById(id);
        return new AdminDeviceVO(updated.getId(), updated.getCode(), updated.getName(), updated.getTotalQuantity(), updated.getStatus(), updated.getLocation(), updated.getDescription(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public void deleteDevice(AuthenticatedUser actor, Long id) {
        DeviceEntity existing = deviceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "设备不存在");
        }
        deviceMapper.deleteById(id);
        adminAuditService.record(actor, AdminAuditActions.DEVICE_DELETE, "device", id, Map.of("code", existing.getCode()));
    }

    public String exportDevicesCsv(AuthenticatedUser actor) {
        List<AdminDeviceVO> devices = listDevices(null, null);
        StringBuilder csv = new StringBuilder();
        csv.append("id,code,name,totalQuantity,status,location,description,createdAt\n");
        for (AdminDeviceVO d : devices) {
            csv.append(AdminCsv.cell(d.id())).append(",");
            csv.append(AdminCsv.cell(d.code())).append(",");
            csv.append(AdminCsv.cell(d.name())).append(",");
            csv.append(AdminCsv.cell(d.totalQuantity())).append(",");
            csv.append(AdminCsv.cell(d.status())).append(",");
            csv.append(AdminCsv.cell(d.location())).append(",");
            csv.append(AdminCsv.cell(d.description())).append(",");
            csv.append(AdminCsv.cell(d.createdAt())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.DEVICE_EXPORT, "device", null, Map.of("count", devices.size()));
        return csv.toString();
    }

    public byte[] exportDevicesExcel(AuthenticatedUser actor) {
        List<AdminDeviceVO> devices = listDevices(null, null);
        var deviceRows = devices.stream()
                .map(d -> row(d.id(), d.code(), d.name(), d.totalQuantity(), d.status(), d.location(), d.description(), d.createdAt(), d.updatedAt()))
                .toList();
        byte[] bytes = excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "设备",
                        List.of("ID", "编码", "名称", "总数量", "状态", "位置", "描述", "创建时间", "更新时间"),
                        deviceRows
                )
        ));
        adminAuditService.record(actor, AdminAuditActions.DEVICE_EXPORT, "device", null, Map.of("count", devices.size(), "format", "excel"));
        return bytes;
    }

    public List<AdminSemesterVO> listSemesters() {
        return semesterMapper.selectList(new LambdaQueryWrapper<SemesterEntity>().orderByAsc(SemesterEntity::getId))
                .stream()
                .map(s -> new AdminSemesterVO(s.getId(), s.getName(), s.getStartDate(), s.getEndDate(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public AdminSemesterVO createSemester(AuthenticatedUser actor, AdminSemesterRequest request) {
        SemesterEntity dup = semesterMapper.selectOne(new LambdaQueryWrapper<SemesterEntity>()
                .eq(SemesterEntity::getName, request.name().trim())
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "学期名称已存在");
        }
        SemesterEntity entity = new SemesterEntity();
        entity.setName(request.name().trim());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        semesterMapper.insert(entity);
        adminAuditService.record(actor, AdminAuditActions.SEMESTER_CREATE, "semester", entity.getId(), Map.of("name", entity.getName()));
        return new AdminSemesterVO(entity.getId(), entity.getName(), entity.getStartDate(), entity.getEndDate(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Transactional
    public AdminSemesterVO updateSemester(AuthenticatedUser actor, Long id, AdminSemesterRequest request) {
        SemesterEntity existing = semesterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "学期不存在");
        }
        SemesterEntity dup = semesterMapper.selectOne(new LambdaQueryWrapper<SemesterEntity>()
                .eq(SemesterEntity::getName, request.name().trim())
                .ne(SemesterEntity::getId, id)
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "学期名称已被占用");
        }
        semesterMapper.update(null, new LambdaUpdateWrapper<SemesterEntity>()
                .eq(SemesterEntity::getId, id)
                .set(SemesterEntity::getName, request.name().trim())
                .set(SemesterEntity::getStartDate, request.startDate())
                .set(SemesterEntity::getEndDate, request.endDate())
                .set(SemesterEntity::getUpdatedAt, LocalDateTime.now()));
        adminAuditService.record(actor, AdminAuditActions.SEMESTER_UPDATE, "semester", id, Map.of("name", request.name().trim()));
        SemesterEntity updated = semesterMapper.selectById(id);
        return new AdminSemesterVO(updated.getId(), updated.getName(), updated.getStartDate(), updated.getEndDate(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public void deleteSemester(AuthenticatedUser actor, Long id) {
        SemesterEntity existing = semesterMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "学期不存在");
        }
        semesterMapper.deleteById(id);
        adminAuditService.record(actor, AdminAuditActions.SEMESTER_DELETE, "semester", id, Map.of("name", existing.getName()));
    }

    public String exportSemestersCsv(AuthenticatedUser actor) {
        List<AdminSemesterVO> semesters = listSemesters();
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,startDate,endDate,createdAt\n");
        for (AdminSemesterVO s : semesters) {
            csv.append(AdminCsv.cell(s.id())).append(",");
            csv.append(AdminCsv.cell(s.name())).append(",");
            csv.append(AdminCsv.cell(s.startDate())).append(",");
            csv.append(AdminCsv.cell(s.endDate())).append(",");
            csv.append(AdminCsv.cell(s.createdAt())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.SEMESTER_EXPORT, "semester", null, Map.of("count", semesters.size()));
        return csv.toString();
    }

    public byte[] exportSemestersExcel(AuthenticatedUser actor) {
        List<AdminSemesterVO> semesters = listSemesters();
        var semesterRows = semesters.stream()
                .map(s -> row(s.id(), s.name(), s.startDate(), s.endDate(), s.createdAt(), s.updatedAt()))
                .toList();
        byte[] bytes = excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "学期",
                        List.of("ID", "名称", "开始日期", "结束日期", "创建时间", "更新时间"),
                        semesterRows
                )
        ));
        adminAuditService.record(actor, AdminAuditActions.SEMESTER_EXPORT, "semester", null, Map.of("count", semesters.size(), "format", "excel"));
        return bytes;
    }

    public PageResult<AdminAuditLogVO> listAuditLogs(
            String action,
            String actorUsername,
            String targetType,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        page = Math.max(1, page);
        size = Math.min(Math.max(1, size), 200);
        int offset = (page - 1) * size;

        LambdaQueryWrapper<AuditLogEntity> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            w.eq(AuditLogEntity::getAction, action.trim());
        }
        if (StringUtils.hasText(actorUsername)) {
            w.like(AuditLogEntity::getActorUsername, actorUsername.trim());
        }
        if (StringUtils.hasText(targetType)) {
            w.eq(AuditLogEntity::getTargetType, targetType.trim());
        }
        if (from != null) {
            w.ge(AuditLogEntity::getCreatedAt, from);
        }
        if (to != null) {
            w.le(AuditLogEntity::getCreatedAt, to);
        }

        long total = auditLogMapper.selectCount(w);
        if (total == 0) {
            return new PageResult<>(page, size, 0, List.of());
        }

        List<AuditLogEntity> logs = auditLogMapper.selectList(w.orderByDesc(AuditLogEntity::getId).last("LIMIT " + size + " OFFSET " + offset));
        List<AdminAuditLogVO> items = logs.stream()
                .map(l -> new AdminAuditLogVO(l.getId(), l.getActorId(), l.getActorUsername(), l.getAction(), l.getTargetType(), l.getTargetId(), l.getDetailJson(), l.getCreatedAt()))
                .toList();
        return new PageResult<>(page, size, total, items);
    }

    public String exportAuditLogsCsv(AuthenticatedUser actor, String action, String actorUsername, String targetType, LocalDateTime from, LocalDateTime to) {
        // Export without paging; still allow filters.
        LambdaQueryWrapper<AuditLogEntity> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            w.eq(AuditLogEntity::getAction, action.trim());
        }
        if (StringUtils.hasText(actorUsername)) {
            w.like(AuditLogEntity::getActorUsername, actorUsername.trim());
        }
        if (StringUtils.hasText(targetType)) {
            w.eq(AuditLogEntity::getTargetType, targetType.trim());
        }
        if (from != null) {
            w.ge(AuditLogEntity::getCreatedAt, from);
        }
        if (to != null) {
            w.le(AuditLogEntity::getCreatedAt, to);
        }
        List<AuditLogEntity> logs = auditLogMapper.selectList(w.orderByDesc(AuditLogEntity::getId));
        List<AdminAuditLogVO> items = logs.stream()
                .map(l -> new AdminAuditLogVO(l.getId(), l.getActorId(), l.getActorUsername(), l.getAction(), l.getTargetType(), l.getTargetId(), l.getDetailJson(), l.getCreatedAt()))
                .toList();
        StringBuilder csv = new StringBuilder();
        csv.append("id,actorId,actorUsername,action,targetType,targetId,createdAt,detailJson\n");
        for (AdminAuditLogVO l : items) {
            csv.append(AdminCsv.cell(l.id())).append(",");
            csv.append(AdminCsv.cell(l.actorId())).append(",");
            csv.append(AdminCsv.cell(l.actorUsername())).append(",");
            csv.append(AdminCsv.cell(l.action())).append(",");
            csv.append(AdminCsv.cell(l.targetType())).append(",");
            csv.append(AdminCsv.cell(l.targetId())).append(",");
            csv.append(AdminCsv.cell(l.createdAt())).append(",");
            csv.append(AdminCsv.cell(l.detailJson())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.AUDIT_EXPORT, "audit_log", null, Map.of("count", items.size()));
        return csv.toString();
    }

    public byte[] exportAuditLogsExcel(AuthenticatedUser actor, String action, String actorUsername, String targetType, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<AuditLogEntity> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(action)) {
            w.eq(AuditLogEntity::getAction, action.trim());
        }
        if (StringUtils.hasText(actorUsername)) {
            w.like(AuditLogEntity::getActorUsername, actorUsername.trim());
        }
        if (StringUtils.hasText(targetType)) {
            w.eq(AuditLogEntity::getTargetType, targetType.trim());
        }
        if (from != null) {
            w.ge(AuditLogEntity::getCreatedAt, from);
        }
        if (to != null) {
            w.le(AuditLogEntity::getCreatedAt, to);
        }
        List<AuditLogEntity> logs = auditLogMapper.selectList(w.orderByDesc(AuditLogEntity::getId));
        List<AdminAuditLogVO> items = logs.stream()
                .map(l -> new AdminAuditLogVO(l.getId(), l.getActorId(), l.getActorUsername(), l.getAction(), l.getTargetType(), l.getTargetId(), l.getDetailJson(), l.getCreatedAt()))
                .toList();
        var auditRows = items.stream()
                .map(l -> row(l.id(), l.actorId(), l.actorUsername(), l.action(), l.targetType(), l.targetId(), l.detailJson(), l.createdAt()))
                .toList();
        byte[] bytes = excelExportService.writeWorkbook(List.of(
                new ExcelExportService.SheetSpec(
                        "审计日志",
                        List.of("ID", "操作者ID", "操作者用户名", "动作", "目标类型", "目标ID", "明细JSON", "创建时间"),
                        auditRows
                )
        ));
        adminAuditService.record(actor, AdminAuditActions.AUDIT_EXPORT, "audit_log", null, Map.of("count", items.size(), "format", "excel"));
        return bytes;
    }

    private List<?> row(Object... values) {
        return java.util.Arrays.asList(values);
    }
}
