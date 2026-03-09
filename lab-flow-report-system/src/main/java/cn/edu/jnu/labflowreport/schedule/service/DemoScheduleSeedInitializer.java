package cn.edu.jnu.labflowreport.schedule.service;

import cn.edu.jnu.labflowreport.persistence.entity.LabRoomEntity;
import cn.edu.jnu.labflowreport.persistence.entity.OrgClassEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SemesterEntity;
import cn.edu.jnu.labflowreport.persistence.entity.SysUserEntity;
import cn.edu.jnu.labflowreport.persistence.mapper.LabRoomMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.OrgClassMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SemesterMapper;
import cn.edu.jnu.labflowreport.persistence.mapper.SysUserMapper;
import cn.edu.jnu.labflowreport.schedule.entity.CourseScheduleEntity;
import cn.edu.jnu.labflowreport.schedule.entity.TimeSlotEntity;
import cn.edu.jnu.labflowreport.schedule.mapper.CourseScheduleMapper;
import cn.edu.jnu.labflowreport.schedule.mapper.TimeSlotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoScheduleSeedInitializer implements ApplicationRunner {

    private final SemesterMapper semesterMapper;
    private final OrgClassMapper orgClassMapper;
    private final LabRoomMapper labRoomMapper;
    private final SysUserMapper sysUserMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final CourseScheduleMapper courseScheduleMapper;

    public DemoScheduleSeedInitializer(
            SemesterMapper semesterMapper,
            OrgClassMapper orgClassMapper,
            LabRoomMapper labRoomMapper,
            SysUserMapper sysUserMapper,
            TimeSlotMapper timeSlotMapper,
            CourseScheduleMapper courseScheduleMapper
    ) {
        this.semesterMapper = semesterMapper;
        this.orgClassMapper = orgClassMapper;
        this.labRoomMapper = labRoomMapper;
        this.sysUserMapper = sysUserMapper;
        this.timeSlotMapper = timeSlotMapper;
        this.courseScheduleMapper = courseScheduleMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SysUserEntity teacher = sysUserMapper.findByUsername("teacher");
        if (teacher == null) {
            teacher = sysUserMapper.findPrimaryTeacher();
        }
        if (teacher == null) {
            return;
        }

        SemesterEntity sem1 = ensureSemester("2025-2026-1", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 1, 16));
        SemesterEntity sem2 = ensureSemester("2025-2026-2", LocalDate.of(2026, 2, 16), LocalDate.of(2026, 6, 30));
        OrgClassEntity softwareClass = findDemoClass(2022, "软件工程1班", "2022级软件工程1班");
        OrgClassEntity chemistryClass = findDemoClass(2022, "化学1班", "2022级化学1班");
        if (softwareClass == null || chemistryClass == null) {
            return;
        }

        ensureRoom("N326", "教学楼 N326", "周一至周五 08:00-21:30");
        ensureRoom("N328", "教学楼 N328", "周一至周五 08:00-21:30");
        ensureTimeSlot("S4", "第7-8节", LocalTime.of(15, 50), LocalTime.of(17, 30));
        ensureTimeSlot("S5", "第10-12节", LocalTime.of(18, 30), LocalTime.of(21, 5));

        seedSemester(sem1, teacher.getId(), softwareClass.getId(), chemistryClass.getId(), sem1Patterns());
        seedSemester(sem2, teacher.getId(), softwareClass.getId(), chemistryClass.getId(), sem2Patterns());
    }

    private SemesterEntity ensureSemester(String name, LocalDate startDate, LocalDate endDate) {
        SemesterEntity entity = semesterMapper.selectOne(new LambdaQueryWrapper<SemesterEntity>()
                .eq(SemesterEntity::getName, name)
                .last("LIMIT 1"));
        if (entity != null) {
            return entity;
        }
        entity = new SemesterEntity();
        entity.setName(name);
        entity.setStartDate(startDate);
        entity.setEndDate(endDate);
        semesterMapper.insert(entity);
        return entity;
    }

    private OrgClassEntity findDemoClass(Integer grade, String name, String legacyName) {
        return orgClassMapper.selectOne(new LambdaQueryWrapper<OrgClassEntity>()
                .and(w -> w.eq(OrgClassEntity::getGrade, grade).eq(OrgClassEntity::getName, name)
                        .or()
                        .eq(OrgClassEntity::getName, legacyName))
                .last("LIMIT 1"));
    }

    private void ensureRoom(String name, String location, String openHours) {
        LabRoomEntity entity = labRoomMapper.selectOne(new LambdaQueryWrapper<LabRoomEntity>()
                .eq(LabRoomEntity::getName, name)
                .last("LIMIT 1"));
        if (entity != null) {
            return;
        }
        entity = new LabRoomEntity();
        entity.setName(name);
        entity.setLocation(location);
        entity.setOpenHours(openHours);
        labRoomMapper.insert(entity);
    }

    private void ensureTimeSlot(String code, String name, LocalTime startTime, LocalTime endTime) {
        TimeSlotEntity entity = timeSlotMapper.selectOne(new LambdaQueryWrapper<TimeSlotEntity>()
                .eq(TimeSlotEntity::getCode, code)
                .last("LIMIT 1"));
        if (entity != null) {
            return;
        }
        entity = new TimeSlotEntity();
        entity.setCode(code);
        entity.setName(name);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        timeSlotMapper.insert(entity);
    }

    private void seedSemester(
            SemesterEntity semester,
            Long teacherId,
            Long softwareClassId,
            Long chemistryClassId,
            List<SchedulePattern> patterns
    ) {
        LocalDate weekStart = semester.getStartDate();
        int weekNo = 1;
        while (!weekStart.isAfter(semester.getEndDate())) {
            for (SchedulePattern pattern : patterns) {
                if (!pattern.matchesWeek(weekNo)) {
                    continue;
                }
                LocalDate lessonDate = weekStart.plusDays(pattern.dayOffset());
                if (lessonDate.isAfter(semester.getEndDate())) {
                    continue;
                }
                Long classId = switch (pattern.classKey()) {
                    case "software" -> softwareClassId;
                    case "chemistry" -> chemistryClassId;
                    default -> null;
                };
                if (classId == null) {
                    continue;
                }
                seedSchedule(semester.getId(), classId, teacherId, pattern.roomName(), lessonDate, pattern.slotCode(), pattern.courseName());
            }
            weekStart = weekStart.plusDays(7);
            weekNo++;
        }
    }

    private void seedSchedule(
            Long semesterId,
            Long classId,
            Long teacherId,
            String roomName,
            LocalDate lessonDate,
            String slotCode,
            String courseName
    ) {
        LabRoomEntity room = labRoomMapper.selectOne(new LambdaQueryWrapper<LabRoomEntity>()
                .eq(LabRoomEntity::getName, roomName)
                .last("LIMIT 1"));
        TimeSlotEntity timeSlot = timeSlotMapper.selectOne(new LambdaQueryWrapper<TimeSlotEntity>()
                .eq(TimeSlotEntity::getCode, slotCode)
                .last("LIMIT 1"));
        if (room == null || timeSlot == null) {
            return;
        }

        Long count = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseScheduleEntity>()
                .eq(CourseScheduleEntity::getSemesterId, semesterId)
                .eq(CourseScheduleEntity::getClassId, classId)
                .eq(CourseScheduleEntity::getLessonDate, lessonDate)
                .eq(CourseScheduleEntity::getSlotId, timeSlot.getId()));
        if (count != null && count > 0) {
            return;
        }

        CourseScheduleEntity entity = new CourseScheduleEntity();
        entity.setSemesterId(semesterId);
        entity.setClassId(classId);
        entity.setTeacherId(teacherId);
        entity.setLabRoomId(room.getId());
        entity.setLessonDate(lessonDate);
        entity.setSlotId(timeSlot.getId());
        entity.setCourseName(courseName);
        courseScheduleMapper.insert(entity);
    }

    private List<SchedulePattern> sem1Patterns() {
        return List.of(
                new SchedulePattern("software", "外语楼 201", "S1", 0, "大学英语一级", null),
                new SchedulePattern("software", "体育馆", "S2", 1, "体育I", null),
                new SchedulePattern("software", "N324", "S5", 2, "高级语言程序设计", Parity.ODD),
                new SchedulePattern("software", "N327", "S2", 3, "中国传统文化概论", Parity.ODD),
                new SchedulePattern("software", "N328", "S4", 3, "中国传统文化概论", Parity.EVEN),
                new SchedulePattern("software", "DF-107", "S4", 4, "线性代数引论", null),
                new SchedulePattern("chemistry", "实验室 A101", "S2", 0, "分析化学", null),
                new SchedulePattern("chemistry", "实验室 B203", "S3", 1, "无机化学", null),
                new SchedulePattern("chemistry", "实验室 A101", "S1", 3, "化工原理", null),
                new SchedulePattern("chemistry", "实验室 B203", "S5", 4, "分析化学实验", Parity.ODD),
                new SchedulePattern("chemistry", "实验室 A101", "S1", 4, "分析化学实验", Parity.EVEN)
        );
    }

    private List<SchedulePattern> sem2Patterns() {
        return List.of(
                new SchedulePattern("software", "N329", "S3", 0, "线性代数引论", null),
                new SchedulePattern("software", "体育馆", "S2", 1, "体育I", null),
                new SchedulePattern("software", "外语楼 201", "S1", 2, "大学英语一级", null),
                new SchedulePattern("software", "N327", "S3", 2, "中国传统文化概论", null),
                new SchedulePattern("software", "N319", "S2", 3, "高等数学I", null),
                new SchedulePattern("software", "N326", "S5", 4, "软件工程导论", Parity.ODD),
                new SchedulePattern("software", "DF-107", "S4", 4, "软件工程导论", Parity.EVEN),
                new SchedulePattern("chemistry", "实验室 B203", "S1", 0, "分析化学实验", null),
                new SchedulePattern("chemistry", "实验室 A101", "S3", 1, "无机化学", null),
                new SchedulePattern("chemistry", "实验室 A101", "S2", 2, "有机化学", Parity.ODD),
                new SchedulePattern("chemistry", "实验室 B203", "S4", 2, "仪器分析实验", Parity.EVEN),
                new SchedulePattern("chemistry", "实验室 A101", "S1", 3, "分析化学", null)
        );
    }

    private record SchedulePattern(
            String classKey,
            String roomName,
            String slotCode,
            int dayOffset,
            String courseName,
            Parity parity
    ) {
        boolean matchesWeek(int weekNo) {
            if (parity == null) {
                return true;
            }
            return switch (parity) {
                case ODD -> weekNo % 2 == 1;
                case EVEN -> weekNo % 2 == 0;
            };
        }
    }

    private enum Parity {
        ODD,
        EVEN
    }
}
