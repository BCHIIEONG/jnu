package cn.edu.jnu.labflowreport.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomOpenSlotRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomOpenSlotVO;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomRequest;
import cn.edu.jnu.labflowreport.admin.dto.AdminLabRoomVO;
import cn.edu.jnu.labflowreport.auth.model.AuthenticatedUser;
import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.persistence.entity.LabRoomEntity;
import cn.edu.jnu.labflowreport.persistence.entity.LabRoomOpenSlotEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomOpenSlotMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabRoomManagementService {

    private final LabRoomMapper labRoomMapper;
    private final LabRoomOpenSlotMapper labRoomOpenSlotMapper;
    private final AdminAuditService adminAuditService;

    public LabRoomManagementService(
            LabRoomMapper labRoomMapper,
            LabRoomOpenSlotMapper labRoomOpenSlotMapper,
            AdminAuditService adminAuditService
    ) {
        this.labRoomMapper = labRoomMapper;
        this.labRoomOpenSlotMapper = labRoomOpenSlotMapper;
        this.adminAuditService = adminAuditService;
    }

    public List<AdminLabRoomVO> listLabRooms() {
        List<LabRoomEntity> rooms = labRoomMapper.selectList(new LambdaQueryWrapper<LabRoomEntity>().orderByAsc(LabRoomEntity::getId));
        if (rooms.isEmpty()) {
            return List.of();
        }
        Map<Long, List<LabRoomOpenSlotEntity>> slotMap = loadSlotMap(
                rooms.stream().map(LabRoomEntity::getId).toList()
        );
        return rooms.stream().map(room -> toVo(room, slotMap.getOrDefault(room.getId(), List.of()))).toList();
    }

    @Transactional
    public AdminLabRoomVO createLabRoom(AuthenticatedUser actor, AdminLabRoomRequest request) {
        LabRoomEntity dup = labRoomMapper.selectOne(new LambdaQueryWrapper<LabRoomEntity>()
                .eq(LabRoomEntity::getName, request.name().trim())
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "实验室名称已存在");
        }
        List<AdminLabRoomOpenSlotRequest> openSlots = normalizeOpenSlots(request.openSlots());
        validateOpenSlots(openSlots);
        LabRoomEntity entity = new LabRoomEntity();
        entity.setName(request.name().trim());
        entity.setLocation(trimToNull(request.location()));
        entity.setOpenHours(buildOpenHoursSummaryFromRequests(openSlots));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        labRoomMapper.insert(entity);
        List<LabRoomOpenSlotEntity> slotEntities = replaceOpenSlots(entity.getId(), openSlots);
        adminAuditService.record(actor, AdminAuditActions.LAB_ROOM_CREATE, "lab_room", entity.getId(), Map.of("name", entity.getName()));
        return toVo(entity, slotEntities);
    }

    @Transactional
    public AdminLabRoomVO updateLabRoom(AuthenticatedUser actor, Long id, AdminLabRoomRequest request) {
        LabRoomEntity existing = labRoomMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验室不存在");
        }
        LabRoomEntity dup = labRoomMapper.selectOne(new LambdaQueryWrapper<LabRoomEntity>()
                .eq(LabRoomEntity::getName, request.name().trim())
                .ne(LabRoomEntity::getId, id)
                .last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "实验室名称已被占用");
        }
        List<AdminLabRoomOpenSlotRequest> openSlots = normalizeOpenSlots(request.openSlots());
        validateOpenSlots(openSlots);
        List<LabRoomOpenSlotEntity> existingSlots = listRoomSlots(id);
        String openHours = openSlots.isEmpty() && existingSlots.isEmpty()
                ? existing.getOpenHours()
                : buildOpenHoursSummaryFromRequests(openSlots);
        existing.setName(request.name().trim());
        existing.setLocation(trimToNull(request.location()));
        existing.setOpenHours(openHours);
        existing.setUpdatedAt(LocalDateTime.now());
        labRoomMapper.updateById(existing);
        List<LabRoomOpenSlotEntity> slotEntities = replaceOpenSlots(id, openSlots);
        adminAuditService.record(actor, AdminAuditActions.LAB_ROOM_UPDATE, "lab_room", id, Map.of("name", existing.getName()));
        return toVo(existing, slotEntities);
    }

    @Transactional
    public void deleteLabRoom(AuthenticatedUser actor, Long id) {
        LabRoomEntity existing = labRoomMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.NOT_FOUND, "实验室不存在");
        }
        try {
            labRoomOpenSlotMapper.delete(new LambdaQueryWrapper<LabRoomOpenSlotEntity>()
                    .eq(LabRoomOpenSlotEntity::getLabRoomId, id));
            labRoomMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ApiCode.BAD_REQUEST, "实验室已被课表或实验课程引用，不能删除");
        }
        adminAuditService.record(actor, AdminAuditActions.LAB_ROOM_DELETE, "lab_room", id, Map.of("name", existing.getName()));
    }

    public String exportLabRoomsCsv(AuthenticatedUser actor) {
        List<AdminLabRoomVO> rooms = listLabRooms();
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,location,openHours,createdAt\n");
        for (AdminLabRoomVO r : rooms) {
            csv.append(AdminCsv.cell(r.id())).append(",");
            csv.append(AdminCsv.cell(r.name())).append(",");
            csv.append(AdminCsv.cell(r.location())).append(",");
            csv.append(AdminCsv.cell(r.openHours())).append(",");
            csv.append(AdminCsv.cell(r.createdAt())).append("\n");
        }
        adminAuditService.record(actor, AdminAuditActions.LAB_ROOM_EXPORT, "lab_room", null, Map.of("count", rooms.size()));
        return csv.toString();
    }

    private Map<Long, List<LabRoomOpenSlotEntity>> loadSlotMap(List<Long> labRoomIds) {
        if (labRoomIds.isEmpty()) {
            return Map.of();
        }
        return labRoomOpenSlotMapper.selectList(new LambdaQueryWrapper<LabRoomOpenSlotEntity>()
                        .in(LabRoomOpenSlotEntity::getLabRoomId, labRoomIds)
                        .orderByAsc(LabRoomOpenSlotEntity::getLabRoomId)
                        .orderByAsc(LabRoomOpenSlotEntity::getWeekday)
                        .orderByAsc(LabRoomOpenSlotEntity::getStartTime))
                .stream()
                .collect(Collectors.groupingBy(LabRoomOpenSlotEntity::getLabRoomId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<LabRoomOpenSlotEntity> listRoomSlots(Long labRoomId) {
        return labRoomOpenSlotMapper.selectList(new LambdaQueryWrapper<LabRoomOpenSlotEntity>()
                .eq(LabRoomOpenSlotEntity::getLabRoomId, labRoomId)
                .orderByAsc(LabRoomOpenSlotEntity::getWeekday)
                .orderByAsc(LabRoomOpenSlotEntity::getStartTime));
    }

    private List<LabRoomOpenSlotEntity> replaceOpenSlots(Long labRoomId, List<AdminLabRoomOpenSlotRequest> requests) {
        labRoomOpenSlotMapper.delete(new LambdaQueryWrapper<LabRoomOpenSlotEntity>()
                .eq(LabRoomOpenSlotEntity::getLabRoomId, labRoomId));
        List<LabRoomOpenSlotEntity> entities = new ArrayList<>();
        for (AdminLabRoomOpenSlotRequest request : requests) {
            LabRoomOpenSlotEntity entity = new LabRoomOpenSlotEntity();
            entity.setLabRoomId(labRoomId);
            entity.setWeekday(request.weekday());
            entity.setStartTime(request.startTime());
            entity.setEndTime(request.endTime());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            labRoomOpenSlotMapper.insert(entity);
            entities.add(entity);
        }
        return entities;
    }

    private void validateOpenSlots(List<AdminLabRoomOpenSlotRequest> openSlots) {
        Map<Integer, List<AdminLabRoomOpenSlotRequest>> grouped = openSlots.stream()
                .collect(Collectors.groupingBy(AdminLabRoomOpenSlotRequest::weekday));
        for (Map.Entry<Integer, List<AdminLabRoomOpenSlotRequest>> entry : grouped.entrySet()) {
            List<AdminLabRoomOpenSlotRequest> slots = entry.getValue().stream()
                    .sorted(Comparator.comparing(AdminLabRoomOpenSlotRequest::startTime))
                    .toList();
            LocalTime lastEnd = null;
            for (AdminLabRoomOpenSlotRequest slot : slots) {
                if (!slot.startTime().isBefore(slot.endTime())) {
                    throw new BusinessException(ApiCode.BAD_REQUEST, "开放时段开始时间必须早于结束时间");
                }
                if (lastEnd != null && slot.startTime().isBefore(lastEnd)) {
                    throw new BusinessException(ApiCode.BAD_REQUEST, "同一实验室同一天的开放时段不能重叠");
                }
                lastEnd = slot.endTime();
            }
        }
    }

    private List<AdminLabRoomOpenSlotRequest> normalizeOpenSlots(List<AdminLabRoomOpenSlotRequest> openSlots) {
        if (openSlots == null) {
            return List.of();
        }
        return openSlots.stream().filter(Objects::nonNull).toList();
    }

    private AdminLabRoomVO toVo(LabRoomEntity room, List<LabRoomOpenSlotEntity> slots) {
        List<AdminLabRoomOpenSlotVO> slotVos = slots.stream()
                .map(slot -> new AdminLabRoomOpenSlotVO(
                        slot.getId(),
                        slot.getWeekday(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getCreatedAt(),
                        slot.getUpdatedAt()
                ))
                .toList();
        String openHours = slotVos.isEmpty() ? room.getOpenHours() : buildOpenHoursSummaryFromVos(slotVos);
        return new AdminLabRoomVO(
                room.getId(),
                room.getName(),
                room.getLocation(),
                openHours,
                slotVos,
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    private String buildOpenHoursSummaryFromRequests(List<AdminLabRoomOpenSlotRequest> openSlots) {
        return buildOpenHoursSummary(openSlots.stream()
                .map(slot -> new OpenSlot(slot.weekday(), slot.startTime(), slot.endTime()))
                .toList());
    }

    private String buildOpenHoursSummaryFromVos(List<AdminLabRoomOpenSlotVO> openSlots) {
        return buildOpenHoursSummary(openSlots.stream()
                .map(slot -> new OpenSlot(slot.weekday(), slot.startTime(), slot.endTime()))
                .toList());
    }

    private String buildOpenHoursSummary(List<OpenSlot> openSlots) {
        if (openSlots == null || openSlots.isEmpty()) {
            return null;
        }
        Map<Integer, List<OpenSlot>> grouped = openSlots.stream()
                .sorted(Comparator.comparingInt(OpenSlotLike::weekday).thenComparing(OpenSlotLike::startTime))
                .collect(Collectors.groupingBy(OpenSlotLike::weekday, LinkedHashMap::new, Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> weekdayLabel(entry.getKey()) + " " + entry.getValue().stream()
                        .map(slot -> slot.startTime() + "-" + slot.endTime())
                        .collect(Collectors.joining("、")))
                .collect(Collectors.joining("；"));
    }

    private String weekdayLabel(int weekday) {
        return switch (weekday) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "未知";
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private interface OpenSlotLike {
        Integer weekday();
        LocalTime startTime();
        LocalTime endTime();
    }

    private record OpenSlot(Integer weekday, LocalTime startTime, LocalTime endTime) implements OpenSlotLike {}
}
