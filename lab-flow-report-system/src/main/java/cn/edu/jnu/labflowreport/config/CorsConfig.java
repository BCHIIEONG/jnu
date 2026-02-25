package cn.edu.jnu.labflowreport.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:}") String allowedOriginPatternsRaw
    ) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> patterns = parsePatterns(allowedOriginPatternsRaw);
        if (patterns.isEmpty()) {
            patterns = List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://192.168.*.*:5173",
                    "http://10.*.*.*:5173",
                    "http://172.16.*.*:5173",
                    "http://172.17.*.*:5173",
                    "http://172.18.*.*:5173",
                    "http://172.19.*.*:5173",
                    "http://172.20.*.*:5173",
                    "http://172.21.*.*:5173",
                    "http://172.22.*.*:5173",
                    "http://172.23.*.*:5173",
                    "http://172.24.*.*:5173",
                    "http://172.25.*.*:5173",
                    "http://172.26.*.*:5173",
                    "http://172.27.*.*:5173",
                    "http://172.28.*.*:5173",
                    "http://172.29.*.*:5173",
                    "http://172.30.*.*:5173",
                    "http://172.31.*.*:5173"
            );
        }
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Trace-Id", "Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> parsePatterns(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return List.of();
        }
        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }
}
