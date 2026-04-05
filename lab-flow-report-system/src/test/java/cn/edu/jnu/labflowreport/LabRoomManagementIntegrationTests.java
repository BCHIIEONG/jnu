package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class LabRoomManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void teacherAndAdminShouldShareSameLabRoomLibrary() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String adminToken = login("admin", "admin123");
        String roomName = "共享实验室_" + Instant.now().getEpochSecond();

        MvcResult created = mockMvc.perform(post("/api/teacher/lab-rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "location":"教学楼 A 区 3 层",
                                  "openSlots":[
                                    {"weekday":1,"startTime":"08:00:00","endTime":"12:00:00"},
                                    {"weekday":3,"startTime":"14:00:00","endTime":"18:00:00"}
                                  ]
                                }
                                """.formatted(roomName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(roomName))
                .andExpect(jsonPath("$.data.openSlots.length()").value(2))
                .andExpect(jsonPath("$.data.openHours").value(containsString("周一")))
                .andReturn();

        Number roomId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(get("/api/admin/lab-rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString(roomName)))
                .andExpect(content().string(containsString(String.valueOf(roomId.longValue()))));
    }

    @Test
    void overlappingOpenSlotsShouldBeRejected() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/api/admin/lab-rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"重叠时段实验室_%s",
                                  "location":"实验楼 B 区",
                                  "openSlots":[
                                    {"weekday":2,"startTime":"08:00:00","endTime":"10:00:00"},
                                    {"weekday":2,"startTime":"09:30:00","endTime":"11:30:00"}
                                  ]
                                }
                                """.formatted(Instant.now().getEpochSecond())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("同一实验室同一天的开放时段不能重叠"));
    }

    @Test
    void adminShouldUpdateDeleteLabRoomAndTeacherMetaShouldSeeOpenHoursSummary() throws Exception {
        String teacherToken = login("teacher", "teacher123");
        String adminToken = login("admin", "admin123");
        String roomName = "实验室开放摘要_" + Instant.now().getEpochSecond();

        MvcResult created = mockMvc.perform(post("/api/admin/lab-rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "location":"实验楼 C 区",
                                  "openSlots":[
                                    {"weekday":4,"startTime":"08:00:00","endTime":"10:00:00"}
                                  ]
                                }
                                """.formatted(roomName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.openHours").value("周四 08:00-10:00"))
                .andReturn();
        long roomId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/admin/lab-rooms/" + roomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "location":"实验楼 C 区 2 层",
                                  "openSlots":[
                                    {"weekday":5,"startTime":"13:00:00","endTime":"17:00:00"}
                                  ]
                                }
                                """.formatted(roomName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location").value("实验楼 C 区 2 层"))
                .andExpect(jsonPath("$.data.openSlots.length()").value(1))
                .andExpect(jsonPath("$.data.openHours").value("周五 13:00-17:00"));

        mockMvc.perform(get("/api/teacher/experiment-courses/meta")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(roomName)))
                .andExpect(content().string(containsString("周五 13:00-17:00")));

        mockMvc.perform(delete("/api/admin/lab-rooms/" + roomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/lab-rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(roomName))));
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
