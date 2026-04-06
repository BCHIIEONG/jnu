package cn.edu.jnu.labflowreport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TaskDiscussionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void discussionUnreadAndAggregateFlowShouldWork() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");
        long[] classIds = fetchClassIds(adminToken);
        long classId = classIds[0];
        long otherClassId = classIds[1];

        String studentA = "disc_stu_a_" + System.currentTimeMillis();
        String studentB = "disc_stu_b_" + System.currentTimeMillis();
        String outsider = "disc_outsider_" + System.currentTimeMillis();
        String otherTeacher = "disc_teacher_" + System.currentTimeMillis();
        createUser(adminToken, studentA, studentA, "student123", classId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, studentB, studentB, "student123", classId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, outsider, outsider, "student123", otherClassId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, otherTeacher, otherTeacher, "teacher123", null, "[\"ROLE_TEACHER\"]", "[" + otherClassId + "]");

        String studentAToken = login(studentA, "student123");
        String studentBToken = login(studentB, "student123");
        String outsiderToken = login(outsider, "student123");
        String otherTeacherToken = login(otherTeacher, "teacher123");

        long taskId = createTask(teacherToken, classId, "讨论测试任务_" + System.currentTimeMillis());

        long normalThreadId = createThread(studentAToken, taskId, "NORMAL", "普通讨论内容");
        mockMvc.perform(get("/api/teacher/discussions/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        long askThreadId = createThread(studentAToken, taskId, "ASK_TEACHER", "老师您好，这里有个问题");

        mockMvc.perform(get("/api/teacher/discussions/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(get("/api/teacher/discussions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].threadId").value(askThreadId))
                .andExpect(jsonPath("$.data[0].taskId").value(taskId))
                .andExpect(jsonPath("$.data[0].studentUsername").value(studentA))
                .andExpect(jsonPath("$.data[0].unreadCount").value(1));

        mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/discussion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherQaThreads.length()").value(1))
                .andExpect(jsonPath("$.data.normalThreads.length()").value(1))
                .andExpect(jsonPath("$.data.teacherQaThreads[0].id").value(askThreadId))
                .andExpect(jsonPath("$.data.normalThreads[0].id").value(normalThreadId));

        mockMvc.perform(post("/api/teacher/tasks/" + taskId + "/discussion/threads/" + askThreadId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/teacher/discussions/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(post("/api/teacher/tasks/" + taskId + "/discussion/threads/" + askThreadId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"请先检查第三步实验现象\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestTeacherReplyPreview").value("请先检查第三步实验现象"));

        mockMvc.perform(get("/api/tasks/discussion/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUnreadCount").value(1))
                .andExpect(jsonPath("$.data.items[0].taskId").value(taskId));

        mockMvc.perform(get("/api/tasks/discussion/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUnreadCount").value(0));

        mockMvc.perform(post("/api/tasks/" + taskId + "/discussion/threads/" + askThreadId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks/discussion/unread-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUnreadCount").value(0));

        mockMvc.perform(get("/api/tasks/" + taskId + "/discussion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherQaThreads[0].latestTeacherReplyPreview").value("请先检查第三步实验现象"));

        mockMvc.perform(get("/api/tasks/" + taskId + "/discussion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/discussion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());
    }

    private long[] fetchClassIds(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        long first = ((Number) JsonPath.read(json, "$.data[0].id")).longValue();
        long second = ((Number) JsonPath.read(json, "$.data[1].id")).longValue();
        return new long[]{first, second};
    }

    private long createTask(String teacherToken, long classId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"description\":\"讨论测试\",\"classIds\":[" + classId + "]}"))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private long createThread(String token, long taskId, String type, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/" + taskId + "/discussion/threads")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"" + type + "\",\"content\":\"" + content + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
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
}
