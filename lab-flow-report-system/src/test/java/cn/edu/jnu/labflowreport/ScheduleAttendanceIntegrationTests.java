package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class ScheduleAttendanceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void scheduleAndAttendanceFlowShouldWork() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        // Pick existing seed semester and class.
        MvcResult semesters = mockMvc.perform(get("/api/admin/semesters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number semesterIdNum = JsonPath.read(semesters.getResponse().getContentAsString(), "$.data[0].id");
        long semesterId = semesterIdNum.longValue();

        MvcResult classes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number classIdNum = JsonPath.read(classes.getResponse().getContentAsString(), "$.data[0].id");
        long classId = classIdNum.longValue();

        MvcResult teacherUsers = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("q", "teacher")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number teacherIdNum = JsonPath.read(teacherUsers.getResponse().getContentAsString(), "$.data.items[0].id");
        long teacherId = teacherIdNum.longValue();

        // Create an extra student in the same class who will NOT check in (to verify "not checked in" export rows).
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student_absent",
                                  "displayName":"Absent Student",
                                  "password":"student123",
                                  "enabled":true,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Create an extra student for dynamic-token check-in (so we can test both static and dynamic flows).
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student_dyn",
                                  "displayName":"Dynamic Student",
                                  "password":"student123",
                                  "enabled":true,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String dynToken = login("student_dyn", "student123");

        // Create a time slot.
        MvcResult slot = mockMvc.perform(post("/api/admin/time-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"TS-T","name":"测试节次","startTime":"08:00","endTime":"09:40"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number slotIdNum = JsonPath.read(slot.getResponse().getContentAsString(), "$.data.id");
        long slotId = slotIdNum.longValue();

        // Create a schedule item for the class/teacher.
        MvcResult schedule = mockMvc.perform(post("/api/admin/course-schedules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semesterId":%d,
                                  "classId":%d,
                                  "teacherId":%d,
                                  "lessonDate":"2026-02-24",
                                  "slotId":%d,
                                  "courseName":"签到演示课"
                                }
                                """.formatted(semesterId, classId, teacherId, slotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number scheduleIdNum = JsonPath.read(schedule.getResponse().getContentAsString(), "$.data.id");
        long scheduleId = scheduleIdNum.longValue();

        // Teacher can query his week schedule.
        mockMvc.perform(get("/api/teacher/schedule/week")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("semesterId", String.valueOf(semesterId))
                        .param("weekStartDate", "2026-02-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString("签到演示课")));

        // Teacher opens a session for that schedule.
        MvcResult session = mockMvc.perform(post("/api/attendance/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleId\":" + scheduleId + ",\"tokenTtlSeconds\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number sessionIdNum = JsonPath.read(session.getResponse().getContentAsString(), "$.data.id");
        long sessionId = sessionIdNum.longValue();

        // Teacher can get a static QR code.
        MvcResult staticRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/static-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String staticCode = JsonPath.read(staticRes.getResponse().getContentAsString(), "$.data.code");

        // Student can check in via static QR.
        mockMvc.perform(post("/api/attendance/checkin/static")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + staticCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(false));

        // Student cannot update token ttl.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/attendance/sessions/" + sessionId + "/token-ttl")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenTtlSeconds\":60}"))
                .andExpect(status().isForbidden());

        // Student cannot fetch tokens.
        mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Teacher fetches token (ttl from session setting).
        MvcResult tokenRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ttlSeconds").value(30))
                .andReturn();

        // Update ttl to 60 and verify.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/attendance/sessions/" + sessionId + "/token-ttl")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenTtlSeconds\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        tokenRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ttlSeconds").value(60))
                .andReturn();

        // Update ttl to 3 and verify token expires.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/attendance/sessions/" + sessionId + "/token-ttl")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenTtlSeconds\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        tokenRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ttlSeconds").value(3))
                .andReturn();
        String tokenExpired = JsonPath.read(tokenRes.getResponse().getContentAsString(), "$.data.token");

        Thread.sleep(3500);

        mockMvc.perform(post("/api/attendance/checkin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dynToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + tokenExpired + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("二维码已过期")));

        // Set ttl back to 60 and perform a normal dynamic check-in + idempotent re-check-in.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/attendance/sessions/" + sessionId + "/token-ttl")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenTtlSeconds\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        tokenRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ttlSeconds").value(60))
                .andReturn();
        String token = JsonPath.read(tokenRes.getResponse().getContentAsString(), "$.data.token");

        mockMvc.perform(post("/api/attendance/checkin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dynToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(false));

        // Second check-in should be idempotent.
        mockMvc.perform(post("/api/attendance/checkin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dynToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(true));

        // Teacher sees records.
        mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString("student")))
                .andExpect(content().string(containsString("student_dyn")));

        // Export CSV should have BOM.
        mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    assertTrue(bytes.length >= 3);
                    assertTrue((bytes[0] & 0xFF) == 0xEF);
                    assertTrue((bytes[1] & 0xFF) == 0xBB);
                    assertTrue((bytes[2] & 0xFF) == 0xBF);
                })
                .andExpect(content().string(containsString("studentUsername")))
                .andExpect(content().string(containsString("student_absent")))
                .andExpect(content().string(containsString("student_dyn")))
                .andExpect(content().string(containsString("NOT_CHECKED_IN")))
                .andExpect(content().string(containsString("CHECKED_IN")));

        mockMvc.perform(post("/api/attendance/sessions/" + sessionId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/teacher/attendance/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("grade", "2022")
                        .param("status", "CLOSED")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(content().string(containsString("签到演示课")));

        mockMvc.perform(get("/api/teacher/attendance/sessions/" + sessionId + "/detail")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalCount").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(content().string(containsString("student_absent")))
                .andExpect(content().string(containsString("NOT_CHECKED_IN")));
    }

    @Test
    void studentWeekScheduleShouldBeClassScoped() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        MvcResult semesters = mockMvc.perform(get("/api/admin/semesters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long semesterId = ((Number) JsonPath.read(semesters.getResponse().getContentAsString(), "$.data[0].id")).longValue();

        MvcResult teacherUsers = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("q", "teacher")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();
        long teacherId = ((Number) JsonPath.read(teacherUsers.getResponse().getContentAsString(), "$.data.items[0].id")).longValue();

        MvcResult studentUsers = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("q", "student")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();
        long studentClassId = ((Number) JsonPath.read(studentUsers.getResponse().getContentAsString(), "$.data.items[0].classId")).longValue();

        mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":1,"grade":2026,"name":"学生课表测试班"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult classes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        int classCount = JsonPath.read(classes.getResponse().getContentAsString(), "$.data.length()");
        long otherClassId = ((Number) JsonPath.read(classes.getResponse().getContentAsString(), "$.data[" + (classCount - 1) + "].id")).longValue();

        mockMvc.perform(post("/api/admin/time-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"TS-STU","name":"学生课表节次","startTime":"10:00","endTime":"11:40"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult slots = mockMvc.perform(get("/api/admin/time-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        int slotCount = JsonPath.read(slots.getResponse().getContentAsString(), "$.data.length()");
        long slotId = ((Number) JsonPath.read(slots.getResponse().getContentAsString(), "$.data[" + (slotCount - 1) + "].id")).longValue();

        mockMvc.perform(post("/api/admin/course-schedules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semesterId":%d,
                                  "classId":%d,
                                  "teacherId":%d,
                                  "lessonDate":"2026-02-24",
                                  "slotId":%d,
                                  "courseName":"学生本人课表"
                                }
                                """.formatted(semesterId, studentClassId, teacherId, slotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/admin/course-schedules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "semesterId":%d,
                                  "classId":%d,
                                  "teacherId":%d,
                                  "lessonDate":"2026-02-25",
                                  "slotId":%d,
                                  "courseName":"其他班级课表"
                                }
                                """.formatted(semesterId, otherClassId, teacherId, slotId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/student/semesters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/student/time-slots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/student/schedule/week")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("semesterId", String.valueOf(semesterId))
                        .param("weekStartDate", "2026-02-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString("学生本人课表")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("其他班级课表"))));

        mockMvc.perform(get("/api/student/schedule/week")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("semesterId", String.valueOf(semesterId))
                        .param("weekStartDate", "2026-02-24"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student_noclass",
                                  "displayName":"No Class Student",
                                  "password":"student123",
                                  "enabled":true,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String noClassToken = login("student_noclass", "student123");

        mockMvc.perform(get("/api/student/schedule/week")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + noClassToken)
                        .param("semesterId", String.valueOf(semesterId))
                        .param("weekStartDate", "2026-02-24"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("当前账号未绑定班级")));
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
}
