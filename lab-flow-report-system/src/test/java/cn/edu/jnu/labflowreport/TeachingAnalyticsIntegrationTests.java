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
class TeachingAnalyticsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void teacherTeachingAnalyticsShouldReturnDashboardAndPersistIssueTags() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        SeededTeachingAnalyticsData seeded = seedTeachingAnalyticsData();
        long submissionId = seeded.submissionId();
        long exportCountBefore = countBySql("SELECT COUNT(*) FROM export_record WHERE export_type = 'TEACHER_TEACHING_EXPERIMENT_ANALYTICS'");

        mockMvc.perform(post("/api/submissions/{submissionId}/review", submissionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 88.5,
                                  "comment": "结构完整，但分析部分还可以再加强",
                                  "issueTags": ["FORMAT", "ANALYSIS"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.issueTags[0]").exists());

        mockMvc.perform(get("/api/submissions/{submissionId}/review", submissionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issueTags").isArray())
                .andExpect(content().string(containsString("FORMAT")))
                .andExpect(content().string(containsString("ANALYSIS")));

        MvcResult dashboard = mockMvc.perform(get("/api/teacher/analytics/teaching")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.semesterId").exists())
                .andExpect(jsonPath("$.data.experimentAnalysis.taskTable").isArray())
                .andExpect(jsonPath("$.data.studentAnalysis.riskStudentTable").isArray())
                .andExpect(jsonPath("$.data.reportQualityAnalysis.issueTagTable").isArray())
                .andReturn();

        String dashboardJson = dashboard.getResponse().getContentAsString();
        Number semesterId = JsonPath.read(dashboardJson, "$.data.filters.semesterId");
        String from = JsonPath.read(dashboardJson, "$.data.filters.from");
        String to = JsonPath.read(dashboardJson, "$.data.filters.to");
        assertNotNull(semesterId);
        assertNotNull(from);
        assertNotNull(to);
        assertTrue(dashboardJson.contains("格式不规范") || dashboardJson.contains("结果分析不足"));

        mockMvc.perform(get("/api/teacher/analytics/teaching/experiment/export/excel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("semesterId", String.valueOf(semesterId.longValue()))
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        long exportCountAfter = countBySql("SELECT COUNT(*) FROM export_record WHERE export_type = 'TEACHER_TEACHING_EXPERIMENT_ANALYTICS'");
        assertEquals(exportCountBefore + 1, exportCountAfter);
    }

    @Test
    void adminTeachingAnalyticsShouldSupportFiltersAndExcelExport() throws Exception {
        String adminToken = login("admin", "admin123");
        SeededTeachingAnalyticsData seeded = seedTeachingAnalyticsData();
        long auditCountBefore = countBySql("SELECT COUNT(*) FROM audit_log WHERE target_type = 'teaching_analytics_report'");

        MvcResult dashboard = mockMvc.perform(get("/api/admin/analytics/teaching")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.filters.semesterId").exists())
                .andExpect(jsonPath("$.data.experimentAnalysis.taskTable").isArray())
                .andExpect(jsonPath("$.data.studentAnalysis.weakTaskTable").isArray())
                .andExpect(jsonPath("$.data.reportQualityAnalysis.issueTagTable").isArray())
                .andReturn();

        String json = dashboard.getResponse().getContentAsString();
        Number semesterId = JsonPath.read(json, "$.data.filters.semesterId");
        String from = JsonPath.read(json, "$.data.filters.from");
        String to = JsonPath.read(json, "$.data.filters.to");
        Number teacherId = seeded.teacherId();
        Number classId = seeded.classId();
        Number studentId = seeded.studentId();
        assertNotNull(semesterId);
        assertNotNull(teacherId);
        assertNotNull(classId);
        assertNotNull(studentId);

        mockMvc.perform(get("/api/admin/analytics/teaching")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("semesterId", String.valueOf(semesterId.longValue()))
                        .param("from", from)
                        .param("to", to)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue()))
                        .param("studentId", String.valueOf(studentId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filters.teacherId").value(teacherId.longValue()))
                .andExpect(jsonPath("$.data.filters.classId").value(classId.longValue()))
                .andExpect(jsonPath("$.data.filters.studentId").value(studentId.longValue()));

        mockMvc.perform(get("/api/admin/analytics/teaching/report-quality/export/excel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("teacherId", String.valueOf(teacherId.longValue()))
                        .param("classId", String.valueOf(classId.longValue()))
                        .param("studentId", String.valueOf(studentId.longValue())))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        long auditCountAfter = countBySql("SELECT COUNT(*) FROM audit_log WHERE target_type = 'teaching_analytics_report'");
        assertTrue(auditCountAfter >= auditCountBefore + 1);
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

    private SeededTeachingAnalyticsData seedTeachingAnalyticsData() {
        long taskId = 99001L;
        long submissionId = 99001L;
        long reviewId = 99001L;
        Long teacherId = jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'teacher'", Long.class);
        MapRow student = jdbcTemplate.queryForObject("""
                SELECT su.id AS student_id, su.class_id AS class_id
                FROM sys_user su
                JOIN sys_user_role ur ON ur.user_id = su.id
                JOIN sys_role sr ON sr.id = ur.role_id
                WHERE sr.code = 'ROLE_STUDENT'
                ORDER BY su.id
                LIMIT 1
                """, (rs, rowNum) -> new MapRow(rs.getLong("student_id"), rs.getLong("class_id")));
        if (teacherId == null || student == null) {
            throw new IllegalStateException("测试数据缺少教师或学生");
        }
        jdbcTemplate.update("DELETE FROM report_review_issue_tag WHERE review_id = ?", reviewId);
        jdbcTemplate.update("DELETE FROM report_review WHERE id = ?", reviewId);
        jdbcTemplate.update("DELETE FROM report_submission WHERE id = ?", submissionId);
        jdbcTemplate.update("DELETE FROM exp_task_target_class WHERE task_id = ?", taskId);
        jdbcTemplate.update("DELETE FROM exp_task WHERE id = ?", taskId);
        jdbcTemplate.update("""
                INSERT INTO exp_task (id, title, description, publisher_id, deadline_at, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, TIMESTAMP '2026-05-01 23:59:00', 'OPEN', TIMESTAMP '2026-03-01 10:00:00', TIMESTAMP '2026-03-01 10:00:00')
                """, taskId, "教学分析测试任务", "用于教学分析集成测试", teacherId);
        jdbcTemplate.update("INSERT INTO exp_task_target_class (task_id, class_id) VALUES (?, ?)", taskId, student.classId());
        jdbcTemplate.update("""
                INSERT INTO report_submission (id, task_id, student_id, version_no, content_md, submit_status, submitted_at, created_at)
                VALUES (?, ?, ?, 1, ?, 'SUBMITTED', TIMESTAMP '2026-03-05 09:30:00', TIMESTAMP '2026-03-05 09:30:00')
                """, submissionId, taskId, student.studentId(), "# 教学分析测试报告");
        return new SeededTeachingAnalyticsData(taskId, submissionId, teacherId, student.classId(), student.studentId());
    }

    private record MapRow(long studentId, long classId) {
    }

    private record SeededTeachingAnalyticsData(long taskId, long submissionId, long teacherId, long classId, long studentId) {
    }
}
