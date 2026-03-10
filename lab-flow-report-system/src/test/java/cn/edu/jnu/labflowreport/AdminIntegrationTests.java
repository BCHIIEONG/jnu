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
                        .content("{\"departmentId\":" + depId + ",\"grade\":2022,\"name\":\"测试班级\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.grade").value(2022))
                .andExpect(jsonPath("$.data.displayName").value("2022级测试班级"))
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

        String renamedUsername = username + "_renamed";

        // Update username/displayName
        mockMvc.perform(put("/api/admin/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"测试用户已改名",
                                  "enabled":true
                                }
                                """.formatted(renamedUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.username").value(renamedUsername))
                .andExpect(jsonPath("$.data.displayName").value("测试用户已改名"));

        login(renamedUsername, "Abc12345");

        mockMvc.perform(put("/api/admin/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(put("/api/admin/users/" + userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"teacher\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

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
                        .content("{\"departmentId\":" + depId + ",\"grade\":2023,\"name\":\"删除测试班级\"}"))
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

    @Test
    void teacherShouldSupportMultipleClassesWhileStudentRemainsSingleClass() throws Exception {
        String adminToken = login("admin", "admin123");

        MvcResult depResult = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"教师多班院系-" + Instant.now().getEpochSecond() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long depId = ((Number) JsonPath.read(depResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult class1 = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2022,\"name\":\"软件工程1班\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long classId1 = ((Number) JsonPath.read(class1.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult class2 = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2022,\"name\":\"软件工程2班\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long classId2 = ((Number) JsonPath.read(class2.getResponse().getContentAsString(), "$.data.id")).longValue();

        String teacherUsername = "tm" + Instant.now().getEpochSecond();
        MvcResult teacherResult = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"Teacher123",
                                  "displayName":"多班教师",
                                  "enabled":true,
                                  "departmentId":%d,
                                  "classIds":[%d,%d],
                                  "roleCodes":["ROLE_TEACHER"]
                                }
                                """.formatted(teacherUsername, depId, classId1, classId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classIds.length()").value(2))
                .andExpect(jsonPath("$.data.classDisplayText").value(containsString("2022级软件工程1班")))
                .andExpect(jsonPath("$.data.classDisplayText").value(containsString("2022级软件工程2班")))
                .andReturn();
        long teacherId = ((Number) JsonPath.read(teacherResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        String teacherToken = login(teacherUsername, "Teacher123");

        mockMvc.perform(get("/api/teacher/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("scope", "mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(content().string(containsString("2022级软件工程1班")))
                .andExpect(content().string(containsString("2022级软件工程2班")));

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("classId", String.valueOf(classId2))
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(greaterThan(0)))
                .andExpect(content().string(containsString(teacherUsername)));

        mockMvc.perform(put("/api/admin/users/" + teacherId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"多班教师已更新",
                                  "departmentId":%d,
                                  "classIds":[%d]
                                }
                                """.formatted(depId, classId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classIds.length()").value(1))
                .andExpect(jsonPath("$.data.classIds[0]").value(classId2));

        mockMvc.perform(delete("/api/admin/classes/" + classId2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仍有教师绑定该班级，禁止删除"));

        String studentUsername = "sm" + Instant.now().getEpochSecond();
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"单班学生",
                                  "enabled":true,
                                  "departmentId":%d,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(studentUsername, depId, classId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(classId1))
                .andExpect(jsonPath("$.data.classIds.length()").value(1));

        mockMvc.perform(get("/api/admin/users/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("classIds")))
                .andExpect(content().string(containsString(teacherUsername)))
                .andExpect(content().string(containsString("2022级软件工程2班")));
    }

    @Test
    void studentUpdateShouldIgnoreCompatibleClassIdsPayload() throws Exception {
        String adminToken = login("admin", "admin123");

        MvcResult depResult = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"学生编辑兼容院系-" + Instant.now().getEpochSecond() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long depId = ((Number) JsonPath.read(depResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult class1 = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2022,\"name\":\"兼容1班\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long classId1 = ((Number) JsonPath.read(class1.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult class2 = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2023,\"name\":\"兼容2班\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long classId2 = ((Number) JsonPath.read(class2.getResponse().getContentAsString(), "$.data.id")).longValue();

        String studentUsername = "sfix" + Instant.now().getEpochSecond();
        MvcResult studentResult = mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"待改学生",
                                  "enabled":true,
                                  "departmentId":%d,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(studentUsername, depId, classId1)))
                .andExpect(status().isOk())
                .andReturn();
        long studentId = ((Number) JsonPath.read(studentResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/admin/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"只改姓名成功",
                                  "departmentId":%d,
                                  "classId":%d
                                }
                                """.formatted(depId, classId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("只改姓名成功"))
                .andExpect(jsonPath("$.data.classId").value(classId1));

        mockMvc.perform(put("/api/admin/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"兼容单元素classIds",
                                  "departmentId":%d,
                                  "classId":%d,
                                  "classIds":[%d]
                                }
                                """.formatted(depId, classId1, classId1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("兼容单元素classIds"))
                .andExpect(jsonPath("$.data.classId").value(classId1))
                .andExpect(jsonPath("$.data.classIds.length()").value(1));

        mockMvc.perform(put("/api/admin/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"改班级成功",
                                  "departmentId":%d,
                                  "classId":%d
                                }
                                """.formatted(depId, classId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("改班级成功"))
                .andExpect(jsonPath("$.data.classId").value(classId2));

        mockMvc.perform(put("/api/admin/users/" + studentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"非法多班级",
                                  "departmentId":%d,
                                  "classIds":[%d,%d]
                                }
                                """.formatted(depId, classId1, classId2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("仅教师账号支持多班级绑定"));
    }

    @Test
    void sameDepartmentShouldAllowSameClassNameAcrossDifferentGrades() throws Exception {
        String adminToken = login("admin", "admin123");

        MvcResult depResult = mockMvc.perform(post("/api/admin/departments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"年级唯一院系-" + Instant.now().getEpochSecond() + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long depId = ((Number) JsonPath.read(depResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2022,\"name\":\"软件工程1班\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2023,\"name\":\"软件工程1班\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("2023级软件工程1班"));

        mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":" + depId + ",\"grade\":2023,\"name\":\"软件工程1班\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("同一院系下已存在相同年级和班级名称的记录"));
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
