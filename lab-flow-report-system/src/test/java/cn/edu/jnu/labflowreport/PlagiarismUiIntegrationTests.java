package cn.edu.jnu.labflowreport;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PlagiarismUiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teacherShouldSeePlagiarismSummaryInDialogs() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        // Pick a class for the extra student.
        MvcResult classes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number classIdNum = JsonPath.read(classes.getResponse().getContentAsString(), "$.data[0].id");
        long classId = classIdNum.longValue();

        // Create another student in the same class.
        mockMvc.perform(post("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"student2",
                                  "displayName":"Second Student",
                                  "password":"student123",
                                  "enabled":true,
                                  "classId":%d,
                                  "roleCodes":["ROLE_STUDENT"]
                                }
                                """.formatted(classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String student2Token = login("student2", "student123");

        // Teacher creates a task.
        MvcResult taskRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"查重演示任务","description":"用于查重摘要与高亮","classIds":[%d]}
                                """.formatted(classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(taskRes.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        // Two students submit very similar content.
        String content1 = """
                # 实验报告
                目的：学习数据库连接与接口联调。
                步骤：先启动后端，再启动前端，然后测试接口。
                结果：接口返回正常，截图如下。
                """;
        String content2 = """
                # 实验报告
                目的：学习数据库连接与接口联调。
                步骤：先启动后端，再启动前端，然后测试接口。
                结果：接口返回正常，截图如下（基本相同）。
                """;

        MvcResult sub1 = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":" + toJsonString(content1) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number sub1IdNum = JsonPath.read(sub1.getResponse().getContentAsString(), "$.data.id");
        long sub1Id = sub1IdNum.longValue();

        MvcResult sub2 = mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":" + toJsonString(content2) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number sub2IdNum = JsonPath.read(sub2.getResponse().getContentAsString(), "$.data.id");
        long sub2Id = sub2IdNum.longValue();

        // Upload similar code attachment for both.
        byte[] code = "public class Demo { int a = 1; }\n".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile codeFile = new MockMultipartFile("file", "Demo.java", "text/plain", code);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/submissions/" + sub1Id + "/attachments")
                        .file(codeFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/submissions/" + sub2Id + "/attachments")
                        .file(codeFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Upload a small png for both (exact same bytes -> should match).
        byte[] png = TestPng.tinyPngBytes();
        MockMultipartFile imgFile = new MockMultipartFile("file", "shot.png", "image/png", png);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/submissions/" + sub1Id + "/attachments")
                        .file(imgFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/submissions/" + sub2Id + "/attachments")
                        .file(imgFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Upload an xlsx which should be skipped.
        MockMultipartFile xlsx = new MockMultipartFile(
                "file",
                "data.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/submissions/" + sub1Id + "/attachments")
                        .file(xlsx)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Run plagiarism for the task.
        mockMvc.perform(post("/api/teacher/tasks/" + taskId + "/plagiarism/run")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.runId").isNumber());

        // Teacher loads summary for submission 1.
        MvcResult summary = mockMvc.perform(get("/api/teacher/submissions/" + sub1Id + "/plagiarism-summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskId").value(taskId))
                .andExpect(jsonPath("$.data.imagesProcessed").isNumber())
                .andReturn();

        String body = summary.getResponse().getContentAsString();
        Number maxScore = JsonPath.read(body, "$.data.maxScore");
        assertTrue(maxScore.doubleValue() > 0.85, "maxScore should be high");

        String topUser = JsonPath.read(body, "$.data.topMatchStudent.username");
        assertNotNull(topUser);
        assertTrue(topUser.contains("student2"));

        // Fragments should exist for highlighting.
        java.util.List<?> fragmentsLists = JsonPath.read(body, "$.data.evidence[?(@.type=='SUBMISSION_TEXT')].detail.fragments");
        assertTrue(fragmentsLists != null && !fragmentsLists.isEmpty(), "should have fragments");

        // xlsx should be skipped.
        java.util.List<String> skippedNames = JsonPath.read(body, "$.data.skippedAttachments[*].fileName");
        assertTrue(skippedNames.stream().anyMatch(n -> n.toLowerCase().contains("data.xlsx")));

        // image artifacts should be processed (tiny png for both submissions).
        Number imagesProcessed = JsonPath.read(body, "$.data.imagesProcessed");
        assertTrue(imagesProcessed.intValue() >= 1, "should have processed image attachments");
    }

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.token");
    }

    private static String toJsonString(String s) {
        if (s == null) return "null";
        String escaped = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    static final class TestPng {
        private TestPng() {
        }

        static byte[] tinyPngBytes() {
            // 1x1 transparent PNG
            return java.util.Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/58HAAMBAQAYnY0AAAAASUVORK5CYII="
            );
        }
    }
}
