package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExperimentCourseIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void experimentCourseShouldSupportEnrollmentTaskVisibilityAndScheduleMerge() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        long departmentId = createDepartment(adminToken, "实验课程测试院系A_" + System.currentTimeMillis());
        long eligibleClassId = createClass(adminToken, departmentId, 2026, "实验课程测试班A");
        long blockedClassId = createClass(adminToken, departmentId, 2026, "实验课程测试班B");

        String eligibleUsername = "ec_student_a_" + System.currentTimeMillis();
        String blockedUsername = "ec_student_b_" + System.currentTimeMillis();
        createUser(adminToken, eligibleUsername, "选课学生A", "student123", eligibleClassId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, blockedUsername, "选课学生B", "student123", blockedClassId, "[\"ROLE_STUDENT\"]", null);

        String eligibleToken = login(eligibleUsername, "student123");
        String blockedToken = login(blockedUsername, "student123");

        TeacherMeta meta = loadTeacherMeta(teacherToken);
        LocalDate lessonDate = LocalDate.now().plusDays(2);
        if (lessonDate.isBefore(LocalDate.now().plusDays(1))) {
            lessonDate = LocalDate.now().plusDays(7);
        }
        String courseTitle = "实验课程主链_" + System.currentTimeMillis();

        MvcResult createCourseResult = mockMvc.perform(post("/api/teacher/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"实验课程主链测试",
                                  "semesterId":%d,
                                  "enrollDeadlineAt":"%s",
                                  "targetClassIds":[%d],
                                  "targetStudentIds":[],
                                  "slots":[
                                    {
                                      "lessonDate":"%s",
                                      "slotId":%d,
                                      "labRoomId":%d,
                                      "capacity":1
                                    }
                                  ]
                                }
                                """.formatted(
                                courseTitle,
                                meta.semesterId(),
                                LocalDateTime.now().plusDays(5).withNano(0),
                                eligibleClassId,
                                lessonDate,
                                meta.slotId(),
                                meta.labRoomId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long courseId = ((Number) JsonPath.read(createCourseResult.getResponse().getContentAsString(), "$.data.id")).longValue();
        long courseSlotId = ((Number) JsonPath.read(createCourseResult.getResponse().getContentAsString(), "$.data.slots[0].id")).longValue();

        mockMvc.perform(get("/api/student/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eligibleToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(courseTitle)));

        mockMvc.perform(get("/api/student/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + blockedToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(courseTitle))));

        mockMvc.perform(post("/api/student/experiment-courses/" + courseId + "/enroll")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eligibleToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId":%d}
                                """.formatted(courseSlotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enrolled").value(true))
                .andExpect(jsonPath("$.data.selectedSlotId").value(courseSlotId));

        mockMvc.perform(post("/api/student/experiment-courses/" + courseId + "/enroll")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + blockedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId":%d}
                                """.formatted(courseSlotId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你不在该实验课程的开放范围内"));

        String taskTitle = "实验课程任务_" + System.currentTimeMillis();
        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"仅通过实验课程可见",
                                  "experimentCourseId":%d
                                }
                                """.formatted(taskTitle, courseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eligibleToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(taskTitle)));

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + blockedToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(taskTitle))));

        LocalDate weekStart = lessonDate.with(DayOfWeek.MONDAY);
        mockMvc.perform(get("/api/student/schedule/week")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eligibleToken)
                        .param("semesterId", String.valueOf(meta.semesterId()))
                        .param("weekStartDate", weekStart.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("EXPERIMENT_COURSE")))
                .andExpect(content().string(containsString(courseTitle)));
    }

    @Test
    void experimentCourseEligibilityUnionAndCapacityShouldWork() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        long departmentId = createDepartment(adminToken, "实验课程测试院系B_" + System.currentTimeMillis());
        long targetClassId = createClass(adminToken, departmentId, 2026, "实验课程资格班A");
        long otherClassId = createClass(adminToken, departmentId, 2026, "实验课程资格班B");

        String classStudentUsername = "ec_class_" + System.currentTimeMillis();
        String directStudentUsername = "ec_direct_" + System.currentTimeMillis();
        createUser(adminToken, classStudentUsername, "班级开放学生", "student123", targetClassId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, directStudentUsername, "指定开放学生", "student123", otherClassId, "[\"ROLE_STUDENT\"]", null);

        String classStudentToken = login(classStudentUsername, "student123");
        String directStudentToken = login(directStudentUsername, "student123");
        long directStudentId = findUserId(adminToken, directStudentUsername);

        TeacherMeta meta = loadTeacherMeta(teacherToken);
        LocalDate lessonDate = LocalDate.now().plusDays(3);
        String courseTitle = "实验课程资格并集_" + System.currentTimeMillis();

        MvcResult createCourseResult = mockMvc.perform(post("/api/teacher/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"%s",
                                  "description":"资格并集与容量测试",
                                  "semesterId":%d,
                                  "enrollDeadlineAt":"%s",
                                  "targetClassIds":[%d],
                                  "targetStudentIds":[%d],
                                  "slots":[
                                    {
                                      "lessonDate":"%s",
                                      "slotId":%d,
                                      "labRoomId":%d,
                                      "capacity":1
                                    }
                                  ]
                                }
                                """.formatted(
                                courseTitle,
                                meta.semesterId(),
                                LocalDateTime.now().plusDays(5).withNano(0),
                                targetClassId,
                                directStudentId,
                                lessonDate,
                                meta.slotId(),
                                meta.labRoomId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long courseId = ((Number) JsonPath.read(createCourseResult.getResponse().getContentAsString(), "$.data.id")).longValue();
        long slotId = ((Number) JsonPath.read(createCourseResult.getResponse().getContentAsString(), "$.data.slots[0].id")).longValue();

        mockMvc.perform(get("/api/student/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + classStudentToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(courseTitle)));

        mockMvc.perform(get("/api/student/experiment-courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directStudentToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(courseTitle)));

        mockMvc.perform(post("/api/student/experiment-courses/" + courseId + "/enroll")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + classStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId":%d}
                                """.formatted(slotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled").value(true));

        mockMvc.perform(post("/api/student/experiment-courses/" + courseId + "/enroll")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + directStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId":%d}
                                """.formatted(slotId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("当前场次名额已满"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private long createDepartment(String adminToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private long createClass(String adminToken, long departmentId, int grade, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"grade":%d,"name":"%s"}
                                """.formatted(departmentId, grade, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private void createUser(
            String adminToken,
            String username,
            String displayName,
            String password,
            Long classId,
            String roleCodesJson,
            String classIdsJson
    ) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("{")
                .append("\"username\":\"").append(username).append("\",")
                .append("\"displayName\":\"").append(displayName).append("\",")
                .append("\"password\":\"").append(password).append("\",")
                .append("\"enabled\":true,")
                .append("\"roleCodes\":").append(roleCodesJson);
        if (classId != null) {
            body.append(",\"classId\":").append(classId);
        }
        if (classIdsJson != null) {
            body.append(",\"classIds\":").append(classIdsJson);
        }
        body.append("}");

        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long findUserId(String adminToken, String username) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Number> ids = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.items[?(@.username=='%s')].id".formatted(username));
        return ids.get(0).longValue();
    }

    private TeacherMeta loadTeacherMeta(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/teacher/experiment-courses/meta")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        long semesterId = ((Number) JsonPath.read(body, "$.data.semesters[0].id")).longValue();
        long slotId = ((Number) JsonPath.read(body, "$.data.timeSlots[0].id")).longValue();
        long labRoomId = ((Number) JsonPath.read(body, "$.data.labRooms[0].id")).longValue();
        return new TeacherMeta(semesterId, slotId, labRoomId);
    }

    private record TeacherMeta(Long semesterId, Long slotId, Long labRoomId) {
    }
}
