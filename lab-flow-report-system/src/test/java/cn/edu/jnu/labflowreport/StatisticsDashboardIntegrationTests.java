package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsDashboardIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherDashboardAndCsvExportsShouldWork() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        long exportCountBefore = countBySql("SELECT COUNT(*) FROM export_record WHERE export_type = 'TEACHER_TASK_STATS'");

        MvcResult dashboard = mockMvc.perform(get("/api/teacher/statistics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.semesterId").exists())
                .andExpect(jsonPath("$.data.summary.taskCount").exists())
                .andExpect(jsonPath("$.data.charts.taskTrend.categories").isArray())
                .andExpect(jsonPath("$.data.charts.attendanceTrend.series").isArray())
                .andExpect(jsonPath("$.data.tables.taskTable").isArray())
                .andExpect(jsonPath("$.data.tables.experimentCourseTable").isArray())
                .andExpect(jsonPath("$.data.tables.deviceRequestTable").isArray())
                .andReturn();

        Number semesterId = JsonPath.read(dashboard.getResponse().getContentAsString(), "$.data.filters.semesterId");
        String from = JsonPath.read(dashboard.getResponse().getContentAsString(), "$.data.filters.from");
        String to = JsonPath.read(dashboard.getResponse().getContentAsString(), "$.data.filters.to");
        assertNotNull(semesterId);
        assertNotNull(from);
        assertNotNull(to);

        mockMvc.perform(get("/api/teacher/statistics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("semesterId", String.valueOf(semesterId.longValue()))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.semesterId").value(semesterId.longValue()))
                .andExpect(jsonPath("$.data.filters.from").value(from))
                .andExpect(jsonPath("$.data.filters.to").value(to));

        mockMvc.perform(get("/api/teacher/statistics/reports/tasks/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("semesterId", String.valueOf(semesterId.longValue()))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("taskId,taskTitle,submissionCount,reviewedSubmissionCount,avgScore,confirmedCompletionCount")));

        mockMvc.perform(get("/api/teacher/statistics/reports/experiment-courses/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("courseId,courseTitle,slotCount,activeEnrollmentCount,attendanceSessionCount")));

        mockMvc.perform(get("/api/teacher/statistics/reports/device-requests/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("taskId,taskTitle,pendingCount,approvedCount,borrowedCount,returnedCount")));

        long exportCountAfter = countBySql("SELECT COUNT(*) FROM export_record WHERE export_type = 'TEACHER_TASK_STATS'");
        assertEquals(exportCountBefore + 1, exportCountAfter);
    }

    @Test
    void adminDashboardFiltersAndCsvExportsShouldWork() throws Exception {
        String adminToken = login("admin", "admin123");
        long auditCountBefore = countBySql("SELECT COUNT(*) FROM audit_log WHERE target_type = 'statistics_report'");

        MvcResult dashboard = mockMvc.perform(get("/api/admin/statistics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.semesterId").exists())
                .andExpect(jsonPath("$.data.summary.teacherCount").exists())
                .andExpect(jsonPath("$.data.summary.studentCount").exists())
                .andExpect(jsonPath("$.data.charts.taskTrend.categories").isArray())
                .andExpect(jsonPath("$.data.tables.teacherTable").isArray())
                .andExpect(jsonPath("$.data.tables.classTable").isArray())
                .andExpect(jsonPath("$.data.tables.experimentCourseTable").isArray())
                .andReturn();

        String json = dashboard.getResponse().getContentAsString();
        Number semesterId = JsonPath.read(json, "$.data.filters.semesterId");
        String from = JsonPath.read(json, "$.data.filters.from");
        String to = JsonPath.read(json, "$.data.filters.to");
        Number teacherId = JsonPath.read(json, "$.data.filters.teachers[0].id");
        Number classId = JsonPath.read(json, "$.data.filters.classes[0].id");
        assertNotNull(semesterId);
        assertNotNull(teacherId);
        assertNotNull(classId);

        mockMvc.perform(get("/api/admin/statistics/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("semesterId", String.valueOf(semesterId.longValue()))
                        .param("from", from)
                        .param("to", to)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.teacherId").value(teacherId.longValue()))
                .andExpect(jsonPath("$.data.filters.classId").value(classId.longValue()));

        mockMvc.perform(get("/api/admin/statistics/reports/teachers/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("teacherId,teacherName,taskCount,submissionCount,reviewedSubmissionCount,attendanceSessionCount,avgAttendanceRate")));

        mockMvc.perform(get("/api/admin/statistics/reports/classes/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("classId,className,studentCount,submissionCount,attendanceSessionCount,avgAttendanceRate")));

        mockMvc.perform(get("/api/admin/statistics/reports/experiment-courses/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("courseId,courseTitle,teacherId,teacherName,activeEnrollmentCount,slotCount,attendanceSessionCount")));

        long auditCountAfter = countBySql("SELECT COUNT(*) FROM audit_log WHERE target_type = 'statistics_report'");
        assertTrue(auditCountAfter >= auditCountBefore + 3);
    }

    private long countBySql(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
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
