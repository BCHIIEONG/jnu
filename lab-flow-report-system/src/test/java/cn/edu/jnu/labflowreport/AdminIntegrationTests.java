package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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
class AdminIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teacherShouldBeForbiddenFromAdminApis() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void adminCrudAndAuditShouldWork() throws Exception {
        String adminToken = login("admin", "admin123");

        // Create department
        MvcResult depResult = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试院系-" + Instant.now().getEpochSecond() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number depIdNum = JsonPath.read(depResult.getResponse().getContentAsString(), "$.data.id");
        long depId = depIdNum.longValue();

        // Create class
        MvcResult classResult = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"name\":\"测试班级\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number classIdNum = JsonPath.read(classResult.getResponse().getContentAsString(), "$.data.id");
        long classId = classIdNum.longValue();

        String username = "u" + Instant.now().getEpochSecond();

        // Create user
        MvcResult userResult = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"测试用户",
                                  "enabled":true,
                                  "departmentId":%d,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(username, depId, classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn();
        Number userIdNum = JsonPath.read(userResult.getResponse().getContentAsString(), "$.data.id");
        long userId = userIdNum.longValue();

        // Reset password and login with new password
        mockMvc.perform(post("/api/admin/users/" + userId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        login(username, "Abc12345");

        // Disable user
        mockMvc.perform(put("/api/admin/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false));

        // Audit log should contain at least one USER_CREATE entry
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("action", "USER_CREATE")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)))
                .andExpect(content().string(containsString(username)));
    }

    @Test
    void usersExportShouldHaveBom() throws Exception {
        String adminToken = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/users/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    assertTrue(bytes.length >= 3);
                    assertTrue((bytes[0] & 0xFF) == 0xEF);
                    assertTrue((bytes[1] & 0xFF) == 0xBB);
                    assertTrue((bytes[2] & 0xFF) == 0xBF);
                })
                .andExpect(content().string(containsString("username")));
    }

    @Test
    void adminShouldDeleteStudentOnlyUser() throws Exception {
        String adminToken = login("admin", "admin123");

        // Create department
        MvcResult depResult = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"删除测试院系-" + Instant.now().getEpochSecond() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number depIdNum = JsonPath.read(depResult.getResponse().getContentAsString(), "$.data.id");
        long depId = depIdNum.longValue();

        // Create class
        MvcResult classResult = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"name\":\"删除测试班级\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number classIdNum = JsonPath.read(classResult.getResponse().getContentAsString(), "$.data.id");
        long classId = classIdNum.longValue();

        String username = "del" + Instant.now().getEpochSecond();

        // Create user
        MvcResult userResult = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"待删除学生",
                                  "enabled":true,
                                  "departmentId":%d,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(username, depId, classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number userIdNum = JsonPath.read(userResult.getResponse().getContentAsString(), "$.data.id");
        long userId = userIdNum.longValue();

        // Delete should succeed for student-only user with no business data
        mockMvc.perform(delete("/api/admin/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Deleting teacher should be rejected
        MvcResult teacherList = mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("q", "teacher")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number teacherId = JsonPath.read(teacherList.getResponse().getContentAsString(), "$.data.items[0].id");
        mockMvc.perform(delete("/api/admin/users/" + teacherId.longValue())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
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
