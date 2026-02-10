package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.controller.dto.PingEchoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public ApiResponse<PingPayload> ping() {
        return ApiResponse.success(new PingPayload("pong"));
    }

    @PostMapping("/echo")
    public ApiResponse<PingPayload> echo(@Valid @RequestBody PingEchoRequest request) {
        return ApiResponse.success(new PingPayload(request.message()));
    }

    public record PingPayload(String message) {
    }
}

