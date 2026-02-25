package cn.edu.jnu.labflowreport.attendance.service;

import cn.edu.jnu.labflowreport.common.api.ApiCode;
import cn.edu.jnu.labflowreport.common.exception.BusinessException;
import cn.edu.jnu.labflowreport.config.SecurityJwtProperties;
import cn.edu.jnu.labflowreport.attendance.vo.AttendanceTokenVO;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AttendanceTokenService {

    private final String secret;
    private final SecureRandom random = new SecureRandom();

    public AttendanceTokenService(
            SecurityJwtProperties jwtProps,
            @Value("${ATT_SECRET:}") String attSecret
    ) {
        String s = attSecret == null ? "" : attSecret.trim();
        if (s.isEmpty()) {
            s = jwtProps.getSecret();
        }
        this.secret = s;
    }

    public AttendanceTokenVO issueToken(Long sessionId, int ttlSeconds) {
        long iat = Instant.now().getEpochSecond();
        String nonce = randomNonce();
        String sig = sign(sessionId, iat, nonce);
        String token = "v1." + sessionId + "." + iat + "." + nonce + "." + sig;

        AttendanceTokenVO vo = new AttendanceTokenVO();
        vo.setToken(token);
        vo.setIssuedAtEpochSec(iat);
        vo.setTtlSeconds(Math.max(1, ttlSeconds));
        return vo;
    }

    public ParsedToken parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "token 不能为空");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 5 || !"v1".equals(parts[0])) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "二维码 token 格式错误");
        }

        long sessionId;
        long issuedAt;
        String nonce = parts[3];
        String sig = parts[4];
        try {
            sessionId = Long.parseLong(parts[1]);
            issuedAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "二维码 token 格式错误");
        }

        String expected = sign(sessionId, issuedAt, nonce);
        if (!constantTimeEquals(expected, sig)) {
            throw new BusinessException(ApiCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "二维码无效，请重新扫码");
        }
        return new ParsedToken(sessionId, issuedAt);
    }

    private String randomNonce() {
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sign(long sessionId, long issuedAt, String nonce) {
        String data = sessionId + "." + issuedAt + "." + nonce;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "服务器生成 token 失败");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    public record ParsedToken(long sessionId, long issuedAtEpochSec) {
    }
}
