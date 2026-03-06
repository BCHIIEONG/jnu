package cn.edu.jnu.labflowreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.plag")
public record PlagiarismProperties(
        double textThreshold,
        double imageThreshold,
        int resultTopK,
        int maxAttachmentsPerSubmission,
        int maxTextAttachmentBytes,
        int maxImageBytes
) {
    public PlagiarismProperties() {
        this(
                0.85,
                0.92,
                30,
                20,
                2 * 1024 * 1024,
                6 * 1024 * 1024
        );
    }
}

