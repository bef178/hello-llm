package pd.cloudapp.llmhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Baked into the WebClient/ConnectionProvider at startup; changes require a restart to take effect.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "llmhub.web-client")
public class WebClientProperties {

    private int connectTimeoutMs = 3000;

    /**
     * 0 disables the response timeout; streaming responses can legitimately run long
     */
    private int responseTimeoutMs = 0;

    private int maxInMemorySizeBytes = 2 * 1024 * 1024;

    /**
     * each in-flight stream holds a connection for its whole duration, size this by expected concurrent streams
     */
    private int maxConnections = 500;

    private int pendingAcquireMaxCount = 1000;

    private int pendingAcquireTimeoutMs = 5000;
}
