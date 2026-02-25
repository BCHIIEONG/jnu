package cn.edu.jnu.labflowreport.controller;

import cn.edu.jnu.labflowreport.common.api.ApiResponse;
import cn.edu.jnu.labflowreport.controller.dto.PingEchoRequest;
import jakarta.validation.Valid;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    @GetMapping("/lan-ips")
    public ApiResponse<LanIpsPayload> lanIps() {
        List<String> ips = detectLanIpv4();
        String recommended = ips.isEmpty() ? null : ips.get(0);
        return ApiResponse.success(new LanIpsPayload(ips, recommended));
    }

    public record PingPayload(String message) {
    }

    public record LanIpsPayload(List<String> ips, String recommended) {
    }

    private static List<String> detectLanIpv4() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) return List.of();

            Set<String> out = new LinkedHashSet<>();
            for (NetworkInterface ni : Collections.list(ifaces)) {
                try {
                    if (!ni.isUp() || ni.isLoopback()) continue;
                } catch (Exception ignored) {
                    continue;
                }

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!(addr instanceof Inet4Address)) continue;
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isMulticastAddress()) continue;
                    if (!addr.isSiteLocalAddress()) continue;
                    out.add(addr.getHostAddress());
                }
            }

            ArrayList<String> list = new ArrayList<>(out);
            list.sort(Comparator.naturalOrder());
            return list;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
