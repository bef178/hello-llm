package pd.cloudapp.llmhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "llmhub")
public class LlmHubProperties {

    private int connectTimeoutMs = 3000;

    /** 0 disables the response timeout; streaming responses can legitimately run long */
    private int responseTimeoutMs = 0;

    private int maxInMemorySizeBytes = 2 * 1024 * 1024;

    /** each in-flight stream holds a connection for its whole duration, size this by expected concurrent streams */
    private int maxConnections = 500;

    private int pendingAcquireMaxCount = 1000;

    private int pendingAcquireTimeoutMs = 5000;

    /** provider name -> upstream config */
    private Map<String, Provider> providers;

    @Getter
    @Setter
    public static class Provider {

        private String endpoint;

        private String apiKey;
    }
}
