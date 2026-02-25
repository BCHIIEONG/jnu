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
                        .content("{\"scheduleId\":" + scheduleId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number sessionIdNum = JsonPath.read(session.getResponse().getContentAsString(), "$.data.id");
        long sessionId = sessionIdNum.longValue();

        // Student cannot fetch tokens.
        mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Teacher fetches token.
        MvcResult tokenRes = mockMvc.perform(get("/api/attendance/sessions/" + sessionId + "/token")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String token = JsonPath.read(tokenRes.getResponse().getContentAsString(), "$.data.token");

        // Student check-in.
        mockMvc.perform(post("/api/attendance/checkin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.alreadyCheckedIn").value(false));

        // Second check-in should be idempotent.
        mockMvc.perform(post("/api/attendance/checkin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
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
                .andExpect(content().string(containsString("student")));

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
                .andExpect(content().string(containsString("NOT_CHECKED_IN")))
                .andExpect(content().string(containsString("CHECKED_IN")));
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
