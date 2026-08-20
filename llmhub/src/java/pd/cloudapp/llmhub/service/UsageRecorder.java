package pd.cloudapp.llmhub.service;

import org.springframework.stereotype.Component;
import pd.cloudapp.llmhub.entity.RequestContext;
import pd.cloudapp.llmhub.util.ContextLogger;

@Component
public class UsageRecorder {

    private static final ContextLogger log = ContextLogger.of(UsageRecorder.class);

    public void onFirstToken(RequestContext context, long ttft) {
        log.info(context, "llmhub metrics: ttft={}", ttft);
    }

    public void onFinishReason(RequestContext context, String finishReason) {
        log.info(context, "llmhub metrics: finishReason={}", finishReason);
    }

    public void onUsage(RequestContext context, Long promptTokens, Long completionTokens, Long totalTokens) {
        log.info(context, "llmhub metrics: promptTokens={}, completionTokens={}, totalTokens={}",
                promptTokens, completionTokens, totalTokens);
    }
}
