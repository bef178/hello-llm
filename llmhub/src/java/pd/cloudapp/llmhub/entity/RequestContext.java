package pd.cloudapp.llmhub.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import pd.cloudapp.llmhub.config.Provider;

@Builder
@AllArgsConstructor
public class RequestContext {

    public final String traceId;
    public final long startTime;

    public LlmHubRequest request;
    public String requestId;
    public String sessionId;
    public String deviceId;

    public String trueProviderName;
    public String trueModelName;

    public Provider trueProvider;
    public long sendToProviderTime;

    public RequestContext(String traceId) {
        this(traceId, System.currentTimeMillis());
    }

    public RequestContext(String traceId, long startTime) {
        this.traceId = traceId != null ? traceId : UUID.randomUUID().toString();
        this.startTime = startTime;
    }
}
