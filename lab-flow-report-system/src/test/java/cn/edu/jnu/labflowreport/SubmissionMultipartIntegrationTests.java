package cn.edu.jnu.labflowreport;

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
class SubmissionMultipartIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void submissionRulesShouldBeEnforced() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String studentToken = login("student", "student123");

        // Teacher creates a task.
        MvcResult taskRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"提交规则测试","description":"multipart + hash"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum = JsonPath.read(taskRes.getResponse().getContentAsString(), "$.data.id");
        long taskId = taskIdNum.longValue();

        // JSON submit ok.
        mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"Hello\\n\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // JSON duplicate should be rejected.
        mockMvc.perform(post("/api/tasks/" + taskId + "/submissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMd\":\"Hello\\n\"}"))
                .andExpect(status().isBadRequest());

        // multipart: empty + no files -> 400
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId + "/submissions/multipart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", "   "))
                .andExpect(status().isBadRequest());

        MockMultipartFile a1 = new MockMultipartFile(
                "files",
                "a.txt",
                "text/plain",
                "A".getBytes(StandardCharsets.UTF_8)
        );

        // prev content was non-empty (Hello), now empty should require confirm even if has new attachment.
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId + "/submissions/multipart")
                        .file(a1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", ""))
                .andExpect(status().isBadRequest());

        MockMultipartFile b1 = new MockMultipartFile(
                "files",
                "b.txt",
                "text/plain",
                "B".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId + "/submissions/multipart")
                        .file(b1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", "")
                        .param("confirmEmptyContent", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // Create another task to verify "first multipart submit can be empty+file without confirm".
        MvcResult taskRes2 = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"提交规则测试2","description":"multipart first submit"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Number taskIdNum2 = JsonPath.read(taskRes2.getResponse().getContentAsString(), "$.data.id");
        long taskId2 = taskIdNum2.longValue();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId2 + "/submissions/multipart")
                        .file(a1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // multipart: no-change + duplicate attachment -> 400
        MockMultipartFile code = new MockMultipartFile(
                "files",
                "code.java",
                "text/plain",
                "public class X {}".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId2 + "/submissions/multipart")
                        .file(code)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", "Same body"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/tasks/" + taskId2 + "/submissions/multipart")
                        .file(code)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .param("contentMd", "Same body"))
                .andExpect(status().isBadRequest());
    }

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.data.token");
    }
}
