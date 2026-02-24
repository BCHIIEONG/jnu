package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.user.username").value("teacher"));
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
