package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
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
class AuthAndWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginAndMeShouldWork() throws Exception {
        String token = login("teacher", "teacher123");
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("teacher"));
    }

    @Test
    void unauthenticatedMeShouldReturnJson401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("未登录或登录已失效"));
    }

    @Test
    void invalidTokenShouldReturnJson401() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("未登录或登录已失效"));
    }

    @Test
    void studentShouldNotCreateTask() throws Exception {
        String studentToken = login("student", "student123");
        mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"forbidden task","description":"x"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void workflowShouldRunThroughTaskSubmissionReviewAndExport() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"M2任务","description":"报告主链测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        MvcResult submitResult = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 我的实验报告\\n内容"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn();
        Number submissionIdNum = JsonPath.read(submitResult.getResponse().getContentAsString(), "$.data.id");
        long submissionId = submissionIdNum.longValue();

        mockMvc.perform(post("/api/submissions/" + submissionId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":92.5,"comment":"完成较好"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(92.5));

        mockMvc.perform(get("/api/submissions/" + submissionId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comment").value("完成较好"));

        mockMvc.perform(get("/api/tasks/" + taskId + "/scores/export")
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
                .andExpect(content().string(containsString("student")));
    }

    @Test
    void attachmentsShouldUploadAndDownloadWithPermission() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"附件任务","description":"附件上传下载测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        MvcResult submitResult = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 报告\\n包含附件"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number submissionIdNum = JsonPath.read(submitResult.getResponse().getContentAsString(), "$.data.id");
        long submissionId = submissionIdNum.longValue();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello attachment".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/submissions/" + submissionId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                .andReturn();
        Number attachmentIdNum = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.id");
        long attachmentId = attachmentIdNum.longValue();

        mockMvc.perform(get("/api/submissions/" + submissionId + "/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].fileName").value("test.txt"));

        mockMvc.perform(get("/api/attachments/" + attachmentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 0));

        mockMvc.perform(get("/api/submissions/" + submissionId + "/content/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    assertTrue(bytes.length >= 3);
                    assertTrue((bytes[0] & 0xFF) == 0xEF);
                    assertTrue((bytes[1] & 0xFF) == 0xBB);
                    assertTrue((bytes[2] & 0xFF) == 0xBF);
                })
                .andExpect(content().string(containsString("# 报告")));
    }

    @Test
    void otherTeacherShouldNotAccessAnotherTeachersSubmissionResources() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        String otherTeacher = "review_guard_" + System.currentTimeMillis();
        createUser(adminToken, otherTeacher, otherTeacher, "teacher123", null, "[\"ROLE_TEACHER\"]", null);
        String otherTeacherToken = login(otherTeacher, "teacher123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"越权提交流测试","description":"教师越权应被拦截"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = ((Number) JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        MvcResult submitResult = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 越权测试\\n正文"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long submissionId = ((Number) JsonPath.read(submitResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        MockMultipartFile file = new MockMultipartFile("file", "guard.txt", "text/plain", "guard".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/submissions/" + submissionId + "/attachments")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();
        long attachmentId = ((Number) JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/submissions/" + submissionId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":90,"comment":"主教师批阅"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/submissions/" + submissionId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":60,"comment":"越权批阅"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/" + submissionId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/" + submissionId + "/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/attachments/" + attachmentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/submissions/" + submissionId + "/content/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskAttachmentsShouldUploadListDownloadAndDelete() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"带资料的任务","description":"任务资料上传测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "guide.txt",
                "text/plain",
                "task guide".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "template.md",
                "text/markdown",
                "# template".getBytes()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
                        .file(file1)
                        .file(file2)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fileName").value("guide.txt"))
                .andReturn();

        Number attachmentIdNum = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.data[0].id");
        long attachmentId = attachmentIdNum.longValue();

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments.length()").value(2));

        mockMvc.perform(get("/api/tasks/" + taskId + "/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/task-attachments/" + attachmentId + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 0))
                .andExpect(result -> assertTrue(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION).contains("guide.txt")));

        mockMvc.perform(delete("/api/tasks/" + taskId + "/attachments/" + attachmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks/" + taskId + "/attachments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].fileName").value("template.md"));
    }

    @Test
    void emptyTaskShouldBeDeletable() throws Exception {
        String teacherToken = login("teacher", "teacher123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"可删除任务","description":"删除测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void teacherShouldUpdateTaskTitle() throws Exception {
        String teacherToken = login("teacher", "teacher123");

        MvcResult createTaskResult = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"旧任务名","description":"更新标题测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(createTaskResult.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/tasks/" + taskId + "/title")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"新任务名"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("新任务名"));
    }

    @Test
    void forceDeleteShouldCascadeAndRespectPermissions() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        MvcResult classesRes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long departmentId = ((Number) JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[0].departmentId")).longValue();

        String className = "删除测试班_" + System.currentTimeMillis();
        MvcResult classCreate = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"grade":2026,"name":"%s"}
                                """.formatted(departmentId, className)))
                .andExpect(status().isOk())
                .andReturn();
        long classId = ((Number) JsonPath.read(classCreate.getResponse().getContentAsString(), "$.data.id")).longValue();

        String studentA = "delstuA_" + System.currentTimeMillis();
        String studentB = "delstuB_" + System.currentTimeMillis();
        String otherTeacher = "delteacher_" + System.currentTimeMillis();
        createUser(adminToken, studentA, studentA, "student123", classId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, studentB, studentB, "student123", classId, "[\"ROLE_STUDENT\"]", null);
        createUser(adminToken, otherTeacher, otherTeacher, "teacher123", null, "[\"ROLE_TEACHER\"]", "[" + classId + "]");
        String studentAToken = login(studentA, "student123");
        String studentBToken = login(studentB, "student123");
        String otherTeacherToken = login(otherTeacher, "teacher123");

        String taskTitle = "RBAC自检任务_删除链路_" + System.currentTimeMillis();
        MvcResult taskRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"强制删除测试","classIds":[%d]}
                                """.formatted(taskTitle, classId)))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = ((Number) JsonPath.read(taskRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        MockMultipartFile taskFile = new MockMultipartFile("files", "qc-guide.txt", "text/plain", "guide".getBytes());
        mockMvc.perform(multipart("/api/tasks/" + taskId + "/attachments")
                        .file(taskFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        MvcResult submissionARes = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 删除测试A\\n内容A"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long submissionAId = ((Number) JsonPath.read(submissionARes.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 删除测试B\\n内容B"}
                                """))
                .andExpect(status().isOk());

        MockMultipartFile reportFile = new MockMultipartFile("file", "answer.txt", "text/plain", "answer".getBytes());
        mockMvc.perform(multipart("/api/submissions/" + submissionAId + "/attachments")
                        .file(reportFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("answer.txt"));

        mockMvc.perform(post("/api/submissions/" + submissionAId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":88.5,"comment":"可删除"}
                                """))
                .andExpect(status().isOk());

        MockMultipartFile progressImage = new MockMultipartFile("files", "step.png", "image/png", "step".getBytes());
        MvcResult progressRes = mockMvc.perform(multipart("/api/tasks/" + taskId + "/progress")
                        .file(progressImage)
                        .param("content", "步骤1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andReturn();
        long progressLogId = ((Number) JsonPath.read(progressRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/tasks/" + taskId + "/completion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"));

        MvcResult deviceRes = mockMvc.perform(post("/api/admin/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEL-DEV-%d","name":"删除测试设备","totalQuantity":2,"status":"AVAILABLE","location":"A101","description":"delete"}
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andReturn();
        long deviceId = ((Number) JsonPath.read(deviceRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/teacher/tasks/" + taskId + "/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"deviceId":%d,"maxQuantity":2}]
                                """.formatted(deviceId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"quantity":1,"note":"删除链路"}
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(post("/api/teacher/tasks/" + taskId + "/plagiarism/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk());

        Path taskAttachmentDir = Path.of("target", "test-uploads", "task-attachments", String.valueOf(taskId));
        Path reportAttachmentDir = Path.of("target", "test-uploads", "report-attachments", String.valueOf(submissionAId));
        Path progressAttachmentDir = Path.of("target", "test-uploads", "task-progress-attachments", String.valueOf(progressLogId));
        long taskAttachmentFilesBeforeDelete = countRegularFiles(taskAttachmentDir);
        long reportAttachmentFilesBeforeDelete = countRegularFiles(reportAttachmentDir);
        long progressAttachmentFilesBeforeDelete = countRegularFiles(progressAttachmentDir);
        assertTrue(taskAttachmentFilesBeforeDelete > 0);
        assertTrue(reportAttachmentFilesBeforeDelete > 0);
        assertTrue(progressAttachmentFilesBeforeDelete > 0);

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(taskTitle))));

        assertTrue(countRegularFiles(taskAttachmentDir) < taskAttachmentFilesBeforeDelete);
        assertTrue(countRegularFiles(reportAttachmentDir) < reportAttachmentFilesBeforeDelete);
        assertTrue(countRegularFiles(progressAttachmentDir) < progressAttachmentFilesBeforeDelete);

        String adminDeleteTitle = "E2E演示任务_管理员删除_" + System.currentTimeMillis();
        MvcResult adminDeleteTask = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"管理员删除测试","classIds":[%d]}
                                """.formatted(adminDeleteTitle, classId)))
                .andExpect(status().isOk())
                .andReturn();
        long adminDeleteTaskId = ((Number) JsonPath.read(adminDeleteTask.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(delete("/api/tasks/" + adminDeleteTaskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
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

    private boolean hasRegularFiles(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.anyMatch(Files::isRegularFile);
        }
    }

    private long countRegularFiles(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }
}
