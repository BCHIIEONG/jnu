package cn.edu.jnu.labflowreport;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class ExperimentFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void experimentFlowAndDeviceBorrowingShouldWorkEndToEnd() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        MvcResult classesRes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long departmentId = ((Number) JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[0].departmentId")).longValue();

        String className = "流程测试班_" + System.currentTimeMillis();
        MvcResult classCreate = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"grade":2026,"name":"%s"}
                                """.formatted(departmentId, className)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long classId = ((Number) JsonPath.read(classCreate.getResponse().getContentAsString(), "$.data.id")).longValue();

        long seed = System.currentTimeMillis() % 10000;
        String student1Username = String.valueOf(20000000 + seed * 100 + 2);
        String student2Username = String.valueOf(20000000 + seed * 100 + 10);

        createStudent(adminToken, student1Username, classId);
        createStudent(adminToken, student2Username, classId);

        String student1Token = login(student1Username, "student123");
        String student2Token = login(student2Username, "student123");

        String taskTitle = "实验流程任务_" + System.currentTimeMillis();
        MvcResult taskRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"实验流程管理测试","classIds":[%d]}
                                """.formatted(taskTitle, classId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long taskId = ((Number) JsonPath.read(taskRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        MockMultipartFile progressImage = new MockMultipartFile(
                "files",
                "step1.png",
                "image/png",
                "fake-png-content".getBytes()
        );

        mockMvc.perform(multipart("/api/tasks/" + taskId + "/progress")
                        .file(progressImage)
                        .param("content", "步骤1：连接实验设备并拍照记录")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.stepNo").value(1));

        mockMvc.perform(multipart("/api/tasks/" + taskId + "/progress")
                        .param("content", "步骤2：完成软件调试并记录结果")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.stepNo").value(2));

        MvcResult myProgress = mockMvc.perform(get("/api/tasks/" + taskId + "/progress/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].stepNo").value(1))
                .andExpect(jsonPath("$.data[1].stepNo").value(2))
                .andReturn();
        assertTrue(myProgress.getResponse().getContentAsString().contains("step1.png"));

        mockMvc.perform(post("/api/tasks/" + taskId + "/completion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRM"));

        MvcResult progressList = mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/progress")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String progressListJson = progressList.getResponse().getContentAsString();
        assertEquals(student1Username, JsonPath.read(progressListJson, "$.data[0].studentUsername"));
        assertEquals(student2Username, JsonPath.read(progressListJson, "$.data[1].studentUsername"));

        mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/progress/" + JsonPath.read(progressListJson, "$.data[0].studentId"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.logs.length()").value(2))
                .andExpect(content().string(containsString("step1.png")));

        mockMvc.perform(post("/api/teacher/tasks/" + taskId + "/completion/" + JsonPath.read(progressListJson, "$.data[0].studentId") + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(get("/api/tasks/" + taskId + "/completion/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        String deviceCode = "DEV-FLOW-" + System.currentTimeMillis();
        MvcResult deviceRes = mockMvc.perform(post("/api/admin/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"%s",
                                  "name":"实验开发板",
                                  "totalQuantity":3,
                                  "status":"AVAILABLE",
                                  "location":"A101",
                                  "description":"流程测试设备"
                                }
                                """.formatted(deviceCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        long deviceId = ((Number) JsonPath.read(deviceRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/teacher/tasks/" + taskId + "/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"deviceId":%d,"maxQuantity":3}]
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        MvcResult taskDevicesRes = mockMvc.perform(get("/api/tasks/" + taskId + "/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String taskDevicesJson = taskDevicesRes.getResponse().getContentAsString();
        int targetIdx = findByDeviceCode(taskDevicesJson, deviceCode);
        assertTrue(targetIdx >= 0);
        assertEquals(3, ((Number) JsonPath.read(taskDevicesJson, "$.data[" + targetIdx + "].configuredQuantity")).intValue());
        assertEquals(3, ((Number) JsonPath.read(taskDevicesJson, "$.data[" + targetIdx + "].availableQuantity")).intValue());

        MvcResult request1Res = mockMvc.perform(post("/api/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"quantity":2,"note":"步骤调试需要"}
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long request1Id = ((Number) JsonPath.read(request1Res.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/teacher/device-requests/" + request1Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/teacher/device-requests/" + request1Id + "/checkout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("BORROWED"));

        mockMvc.perform(post("/api/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"quantity":2,"note":"库存验证"}
                                """.formatted(deviceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value(containsString("库存不足或超过任务允许数量")));

        mockMvc.perform(post("/api/teacher/device-requests/" + request1Id + "/return")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RETURNED"));

        MvcResult request2Res = mockMvc.perform(post("/api/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + student2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"quantity":2,"note":"第二位学生申请"}
                                """.formatted(deviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        long request2Id = ((Number) JsonPath.read(request2Res.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(post("/api/teacher/device-requests/" + request2Id + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .param("q", student1Username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(content().string(containsString(student1Username)));

        mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/device-requests/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] bytes = result.getResponse().getContentAsByteArray();
                    assertTrue(bytes.length >= 3);
                    assertTrue((bytes[0] & 0xFF) == 0xEF);
                    assertTrue((bytes[1] & 0xFF) == 0xBB);
                    assertTrue((bytes[2] & 0xFF) == 0xBF);
                })
                .andExpect(content().string(containsString(deviceCode)))
                .andExpect(content().string(containsString(student1Username)))
                .andExpect(content().string(containsString(student2Username)))
                .andExpect(content().string(containsString("RETURNED")))
                .andExpect(content().string(containsString("REJECTED")));
    }

    @Test
    void concurrentDeviceRequestsShouldNotOversubscribeInventory() throws Exception {
        String adminToken = login("admin", "admin123");
        String teacherToken = login("teacher", "teacher123");

        MvcResult classesRes = mockMvc.perform(get("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        long departmentId = ((Number) JsonPath.read(classesRes.getResponse().getContentAsString(), "$.data[0].departmentId")).longValue();

        String className = "并发库存班_" + System.currentTimeMillis();
        MvcResult classCreate = mockMvc.perform(post("/api/admin/classes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"grade":2026,"name":"%s"}
                                """.formatted(departmentId, className)))
                .andExpect(status().isOk())
                .andReturn();
        long classId = ((Number) JsonPath.read(classCreate.getResponse().getContentAsString(), "$.data.id")).longValue();

        long seed = System.currentTimeMillis() % 10000;
        String student1Username = String.valueOf(30000000 + seed * 10 + 1);
        String student2Username = String.valueOf(30000000 + seed * 10 + 2);
        createStudent(adminToken, student1Username, classId);
        createStudent(adminToken, student2Username, classId);

        String student1Token = login(student1Username, "student123");
        String student2Token = login(student2Username, "student123");

        MvcResult taskRes = mockMvc.perform(post("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"并发设备任务_%d","description":"库存并发保护","classIds":[%d]}
                                """.formatted(System.currentTimeMillis(), classId)))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = ((Number) JsonPath.read(taskRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        String deviceCode = "DEV-CONC-" + System.currentTimeMillis();
        MvcResult deviceRes = mockMvc.perform(post("/api/admin/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"并发测试设备","totalQuantity":3,"status":"AVAILABLE","location":"A102","description":"并发库存验证"}
                                """.formatted(deviceCode)))
                .andExpect(status().isOk())
                .andReturn();
        long deviceId = ((Number) JsonPath.read(deviceRes.getResponse().getContentAsString(), "$.data.id")).longValue();

        mockMvc.perform(put("/api/teacher/tasks/" + taskId + "/devices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"deviceId":%d,"maxQuantity":3}]
                                """.formatted(deviceId)))
                .andExpect(status().isOk());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> f1 = pool.submit(() -> submitDeviceRequest(taskId, deviceId, student1Token, start));
            Future<Integer> f2 = pool.submit(() -> submitDeviceRequest(taskId, deviceId, student2Token, start));
            start.countDown();

            int status1 = f1.get();
            int status2 = f2.get();
            assertNotEquals(status1, status2);
            assertTrue((status1 == 200 && status2 == 400) || (status1 == 400 && status2 == 200));

            MvcResult listRes = mockMvc.perform(get("/api/teacher/tasks/" + taskId + "/device-requests")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken))
                    .andExpect(status().isOk())
                    .andReturn();
            List<Integer> quantities = JsonPath.read(listRes.getResponse().getContentAsString(), "$.data[*].quantity");
            int reserved = quantities.stream().mapToInt(Integer::intValue).sum();
            assertEquals(2, reserved);
        } finally {
            pool.shutdownNow();
        }
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

    private int findByDeviceCode(String json, String deviceCode) {
        Integer length = JsonPath.read(json, "$.data.length()");
        for (int i = 0; i < length; i++) {
            String code = JsonPath.read(json, "$.data[" + i + "].deviceCode");
            if (deviceCode.equals(code)) return i;
        }
        return -1;
    }

    private int submitDeviceRequest(long taskId, long deviceId, String token, CountDownLatch start) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/tasks/" + taskId + "/device-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"quantity":2,"note":"并发申请"}
                                """.formatted(deviceId)))
                .andReturn()
                .getResponse()
                .getStatus();
    }
}
