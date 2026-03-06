package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class TaskClassPlagiarismIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tasksShouldSupportClassTargetingCloseAndPlagiarismAllVersions() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        // Ensure we have two classes.
        MvcResult classesRes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        Number classAIdNum = JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[0].id");
        Number deptIdNum = JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[0].departmentId");
        long classAId = classAIdNum.longValue();

        Long classBId = null;
        try {
            Number classBIdNum = JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[1].id");
            classBId = classBIdNum.longValue();
        } catch (Exception ignore) {
            // Create class B if seed only has one.
            String name = "测试班B_" + System.currentTimeMillis();
            MvcResult createClass = mockMvc.perform(post("/api/admin/classes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"departmentId":%d,"name":"%s"}
                                    """.formatted(deptIdNum.longValue(), name)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            Number classBIdNum2 = JsonPath.read(createClass.getResponse().getContentAsString(), "$.data.id");
            classBId = classBIdNum2.longValue();
        }

        // Create two students in different classes.
        String studentAUser = "studentA_" + System.currentTimeMillis();
        String studentBUser = "studentB_" + System.currentTimeMillis();
        createStudent(adminToken, studentAUser, classAId);
        createStudent(adminToken, studentBUser, classBId);
        String studentAToken = login(studentAUser, "student123");
        String studentBToken = login(studentBUser, "student123");

        // Teacher creates a task for class A only.
        String taskTitleA = "按班发布A_" + System.currentTimeMillis();
        MvcResult taskARes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"only class A","classIds":[%d]}
                                """.formatted(taskTitleA, classAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskAIdNum = JsonPath.read(taskARes.getResponse().getContentAsString(), "$.data.id");
        long taskAId = taskAIdNum.longValue();

        // Student A can see it; student B cannot.
        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString(taskTitleA)));

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(not(containsString(taskTitleA))));

        // Student B cannot submit to class A task.
        mockMvc.perform(post("/api/tasks/" + taskAId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // Student A submits v1.
        MvcResult subA1 = mockMvc.perform(post("/api/tasks/" + taskAId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"# v1\\nhello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number subA1IdNum = JsonPath.read(subA1.getResponse().getContentAsString(), "$.data.id");
        long subA1Id = subA1IdNum.longValue();

        // Close task, then student A cannot submit new versions.
        mockMvc.perform(put("/api/tasks/" + taskAId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(post("/api/tasks/" + taskAId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"# v2\\nhello2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(containsString("任务已关闭")));

        // Attachment upload: deadlineAt set -> allow before deadline even if CLOSED.
        String taskTitleDeadline = "按截止补交_" + System.currentTimeMillis();
        String deadline = LocalDateTime.now().plusMinutes(5).withNano(0).toString();
        MvcResult taskDRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"deadline allow attachments","deadlineAt":"%s","classIds":[%d]}
                                """.formatted(taskTitleDeadline, deadline, classAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long taskDId = ((Number) JsonPath.read(taskDRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult subD1 = mockMvc.perform(post("/api/tasks/" + taskDId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"# deadline\\ncontent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long subD1Id = ((Number) JsonPath.read(subD1.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/tasks/" + taskDId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MockMultipartFile att1 = new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/submissions/" + subD1Id + "/attachments")
                        .file(att1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("a.txt"));

        // Attachment upload: deadlineAt null -> CLOSED blocks.
        String taskTitleNoDeadline = "无截止不补交_" + System.currentTimeMillis();
        MvcResult taskNRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"no deadline","classIds":[%d]}
                                """.formatted(taskTitleNoDeadline, classAId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long taskNId = ((Number) JsonPath.read(taskNRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult subN1 = mockMvc.perform(post("/api/tasks/" + taskNId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"# n\\ncontent\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long subN1Id = ((Number) JsonPath.read(subN1.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/tasks/" + taskNId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MockMultipartFile att2 = new MockMultipartFile("file", "b.txt", "text/plain", "world".getBytes());
        mockMvc.perform(multipart("/api/submissions/" + subN1Id + "/attachments")
                        .file(att2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(containsString("任务已关闭")));

        // Plagiarism: include all versions and exclude same-student matching.
        String taskTitlePlag = "查重全版本_" + System.currentTimeMillis();
        MvcResult taskPRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"plag all versions","classIds":[%d,%d]}
                                """.formatted(taskTitlePlag, classAId, classBId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long taskPId = ((Number) JsonPath.read(taskPRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        String common = "相同句子A。相同句子B。相同句子C。";
        MvcResult pSubA1 = mockMvc.perform(post("/api/tasks/" + taskPId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"" + common + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long pSubA1Id = ((Number) JsonPath.read(pSubA1.getResponse().getContentAsString(), "$.data.id")).longValue();

        // v2: same content but with a NEW attachment (allowed by multipart rule).
        MockMultipartFile newAtt = new MockMultipartFile("files", "new.txt", "text/plain", "new file".getBytes());
        MvcResult pSubA2 = mockMvc.perform(multipart("/api/tasks/" + taskPId + "/submissions/multipart")
                        .file(newAtt)
                        .param("contentMd", common)
                        .param("confirmEmptyContent", "false")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andReturn();
        long pSubA2Id = ((Number) JsonPath.read(pSubA2.getResponse().getContentAsString(), "$.data.id")).longValue();
        assertTrue(pSubA2Id != pSubA1Id);

        // Student B submits similar content.
        MvcResult pSubB1 = mockMvc.perform(post("/api/tasks/" + taskPId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"" + common + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long pSubB1Id = ((Number) JsonPath.read(pSubB1.getResponse().getContentAsString(), "$.data.id")).longValue();
        assertTrue(pSubB1Id > 0);

        mockMvc.perform(post("/api/teacher/tasks/" + taskPId + "/plagiarism/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // v2 should not match to student A v1; it should match to student B.
        mockMvc.perform(get("/api/teacher/submissions/" + pSubA2Id + "/plagiarism-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.maxScore").value(org.hamcrest.Matchers.greaterThan(0.9)))
                .andExpect(jsonPath("$.data.topMatchStudent.username").value(studentBUser));

        // History: student A submits v1 (high) and v2 (low). Teacher opens v2 and should still see v1 risk.
        String taskTitleHist = "查重历史_" + System.currentTimeMillis();
        MvcResult taskHRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"plag history","classIds":[%d,%d]}
                                """.formatted(taskTitleHist, classAId, classBId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long taskHId = ((Number) JsonPath.read(taskHRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        String copyText = "抄袭句子1。抄袭句子2。抄袭句子3。";
        MvcResult hSubA1 = mockMvc.perform(post("/api/tasks/" + taskHId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"" + copyText + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long hSubA1Id = ((Number) JsonPath.read(hSubA1.getResponse().getContentAsString(), "$.data.id")).longValue();
        assertTrue(hSubA1Id > 0);

        MvcResult hSubB1 = mockMvc.perform(post("/api/tasks/" + taskHId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"" + copyText + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long hSubB1Id = ((Number) JsonPath.read(hSubB1.getResponse().getContentAsString(), "$.data.id")).longValue();
        assertTrue(hSubB1Id > 0);

        String cleanText = "这是一份完全不同的内容，不应与他人高度相似。";
        MvcResult hSubA2 = mockMvc.perform(post("/api/tasks/" + taskHId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"" + cleanText + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andReturn();
        long hSubA2Id = ((Number) JsonPath.read(hSubA2.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/teacher/tasks/" + taskHId + "/plagiarism/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult histRes = mockMvc.perform(get("/api/teacher/submissions/" + hSubA2Id + "/plagiarism-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.currentVersionNo").value(2))
                .andExpect(jsonPath("$.data.versions.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.versions[0].versionNo").value(1))
                .andExpect(jsonPath("$.data.versions[1].versionNo").value(2))
                .andExpect(jsonPath("$.data.versions[0].topMatchStudent.username").value(studentBUser))
                .andReturn();

        Double v1Score = JsonPath.read(histRes.getResponse().getContentAsString(), "$.data.versions[0].maxScore");
        Double v2Score = JsonPath.read(histRes.getResponse().getContentAsString(), "$.data.versions[1].maxScore");
        assertTrue(v1Score != null && v2Score != null);
        assertTrue(v1Score > v2Score);

        Double maxEarlier = JsonPath.read(histRes.getResponse().getContentAsString(), "$.data.maxEarlierVersions");
        assertTrue(maxEarlier != null);
        assertEquals(v1Score, maxEarlier, 0.0001);
    }

    private void createStudent(String adminToken, String username, long classId) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "displayName":"%s",
                                  "password":"student123",
                                  "enabled":true,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(username, username, classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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
