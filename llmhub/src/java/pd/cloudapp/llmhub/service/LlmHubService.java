package pd.cloudapp.llmhub.service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pd.cloudapp.llmhub.config.LlmHubProperties;
import pd.cloudapp.llmhub.entity.LlmHubRequest;
import pd.cloudapp.llmhub.entity.LlmHubResponse;
import pd.cloudapp.llmhub.entity.RequestContext;
import pd.cloudapp.llmhub.sse.SseFrameBuilder;
import pd.cloudapp.llmhub.util.ContextLogger;
import pd.cloudapp.llmhub.util.HashUtil;
import pd.cloudapp.llmhub.util.JsonUtil;
import pd.cloudapp.llmhub.util.TimeUtil;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

@Service
@RequiredArgsConstructor
public class LlmHubService {

    private static final ContextLogger log = ContextLogger.of(LlmHubService.class);

    private final WebClient upstreamWebClient;
    private final LlmHubProperties llmHubProperties;
    private final RouterConfig routerConfig;
    private final UsageRecorder usageRecorder;

    public Flux<String> streamChatCompletions(RequestContext context) {
        LlmHubRequest request = context.request;
        if (request.openai == null) {
            return Flux.error(new IllegalArgumentException("openai field must not be null"));
        }

        log.info(context, "llmhub request instrumentation: requestId={}, sessionId={}, deviceId={}, provider={}, openai={}",
                context.requestId, context.sessionId, context.deviceId, request.provider, request.openai);

        route(context);

        log.info(context, "llmhub router: trueProvider={}, trueModel={}",
                context.trueProviderName, context.trueModelName);

        applyOverrides(context);

        byte[] upstreamBodyBytes = serializeUpstreamRequestBody(context.request.openai);
        // headers are intentionally omitted from this log so the upstream api key never ends up in log files
        log.info(context, "llmhub upstream request: url={}, body={}",
                context.trueProvider.getEndpoint(), new String(upstreamBodyBytes, StandardCharsets.UTF_8));

        AtomicBoolean firstTokenSeen = new AtomicBoolean(false);
        AtomicBoolean firstFrameSent = new AtomicBoolean(false);
        AtomicLong providerRecvTime = new AtomicLong();
        return upstreamWebClient.post()
                .uri(context.trueProvider.getEndpoint())
                .headers(headers -> buildUpstreamHeaders(headers, context))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(upstreamBodyBytes)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(subscription -> log.info(context, "llmhub upstream subscribed: {}", TimeUtil.toUtcString(System.currentTimeMillis())))
                .doOnNext(rawChunk -> providerRecvTime.set(System.currentTimeMillis()))
                .doOnNext(rawChunk -> handleUpstreamChunk(context, firstTokenSeen, rawChunk))
                .filter(rawChunk -> rawChunk != null && !rawChunk.isEmpty())
                .map(rawChunk -> buildResponseFrame(context, firstFrameSent, providerRecvTime.get(), rawChunk))
                .doOnNext(frame -> log.debug(context, "llmhub response frame instrumentation: time={}, frame={}",
                        TimeUtil.toUtcString(System.currentTimeMillis()), frame))
                .onErrorResume(WebClientResponseException.class, e -> Flux.just(
                        buildErrorFrame(context, "upstream error: " + e.getStatusCode().value(), firstFrameSent, providerRecvTime.get())))
                .onErrorResume(e -> Flux.just(
                        buildErrorFrame(context, "internal error: " + e.getMessage(), firstFrameSent, providerRecvTime.get())))
                // fires once our Flux is done emitting (complete/error/cancel); Netty may still be flushing the
                // last bytes to the client socket after this, so it's "handed off", not strictly "received by caller"
                .doFinally(signalType -> log.info(context, "llmhub response handed off to caller: signal={}, time={}",
                        signalType, TimeUtil.toUtcString(System.currentTimeMillis())))
                // propagates context down to WebClientConfig's HttpClient.doOnRequest, which stamps providerSendTime at actual send time
                .contextWrite(Context.of(RequestContext.class, context));
    }

    private void route(RequestContext context) {
        String requestProvider = context.request.provider;
        String providerName;
        if (requestProvider != null && !requestProvider.isEmpty()) {
            providerName = requestProvider;
        } else {
            // TODO override rules here: black-list based, percent based, error rate based, etc
            providerName = routerConfig.getDefaultProvider();
        }

        LlmHubProperties.Provider provider = providerName == null || llmHubProperties.getProviders() == null
                ? null
                : llmHubProperties.getProviders().get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("unknown provider: " + providerName);
        }
        context.trueProviderName = providerName;
        context.trueProvider = provider;

        Object modelObject = context.request.openai.get("model");
        String modelName = modelObject == null ? null : modelObject.toString();
        Map<String, Object> overridesConfig = routerConfig.getOverridesConfig(providerName);
        if (overridesConfig != null) {
            Object modelOverride = overridesConfig.get("model");
            if (modelOverride != null && !modelOverride.toString().isEmpty()) {
                modelName = modelOverride.toString();
            }
        }
        context.trueModelName = modelName;
    }

    private void applyOverrides(RequestContext context) {
        Map<String, Object> openai = context.request.openai;
        if (!Objects.equals(openai.get("model"), context.trueModelName)) {
            openai.put("model", context.trueModelName);
        }
        openai.put("stream", Boolean.TRUE);
        openai.put("stream_options", Collections.singletonMap("include_usage", Boolean.TRUE));
    }

    private byte[] serializeUpstreamRequestBody(Map<String, Object> openai) {
        return JsonUtil.writeValueAsBytes(openai);
    }

    private void buildUpstreamHeaders(HttpHeaders headers, RequestContext context) {
        LlmHubProperties.Provider provider = context.trueProvider;
        if (provider.getApiKey() != null && !provider.getApiKey().isEmpty()) {
            headers.setBearerAuth(provider.getApiKey());
        }
        if (context.requestId != null) {
            headers.set("X-Request-ID", context.requestId);
        }
        if (context.sessionId != null) {
            headers.set("X-Session-ID", context.sessionId);
        }
        String checksum = HashUtil.md5Hex(context.deviceId);
        if (checksum != null) {
            headers.set("X-Device-ID", checksum);
        }
    }

    private void handleUpstreamChunk(RequestContext context, AtomicBoolean firstTokenSeen, String rawChunk) {
        log.debug(context, "llmhub upstream response chunk: chunk={}", rawChunk);
        if (firstTokenSeen.compareAndSet(false, true)) {
            usageRecorder.onFirstToken(context, System.currentTimeMillis() - context.startTime);
        }
        // cheap pre-check to save CPU
        if (rawChunk.contains("finish_reason") || rawChunk.contains("\"usage\"")) {
            try {
                JsonNode node = JsonUtil.readTree(rawChunk);
                for (JsonNode choice : node.path("choices")) {
                    JsonNode finishReason = choice.path("finish_reason");
                    if (finishReason.isTextual()) {
                        usageRecorder.onFinishReason(context, finishReason.asText());
                    }
                }
                JsonNode usage = node.path("usage");
                if (usage.isObject()) {
                    usageRecorder.onUsage(context, asLongOrNull(usage.path("prompt_tokens")),
                            asLongOrNull(usage.path("completion_tokens")), asLongOrNull(usage.path("total_tokens")));
                }
            } catch (Exception e) {
                // catch: metrics failed never break the stream
                log.warn(context, "llmhub failed to parse usage signals: error={}", e.getMessage());
            }
        }
    }

    private Long asLongOrNull(JsonNode node) {
        return node.isNumber() ? node.longValue() : null;
    }

    private String buildResponseFrame(RequestContext context, AtomicBoolean firstFrameSent, long providerRecvTime,
            String rawChunk) {
        LlmHubResponse response = new LlmHubResponse();
        response.openai = SseFrameBuilder.DONE_MARKER.equals(rawChunk)
                ? SseFrameBuilder.quoteJsonString(SseFrameBuilder.DONE_MARKER)
                : rawChunk;
        response.trace = buildTrace(context, firstFrameSent, providerRecvTime);
        return JsonUtil.writeValueAsString(response);
    }

    private Map<String, Object> buildTrace(RequestContext context, AtomicBoolean firstFrameSent, long providerRecvTime) {
        Map<String, Object> trace = new LinkedHashMap<>();
        if (firstFrameSent.compareAndSet(false, true)) {
            trace.put("traceId", context.traceId);
            trace.put("provider", context.trueProviderName);
            trace.put("model", context.trueModelName);
            trace.put("startTime", TimeUtil.toUtcString(context.startTime));
            trace.put("providerSendTime", TimeUtil.toUtcString(context.providerSendTime));
        }
        trace.put("providerRecvTime", TimeUtil.toUtcString(providerRecvTime));
        trace.put("endTime", TimeUtil.toUtcString(System.currentTimeMillis()));
        return trace;
    }

    private String buildErrorFrame(RequestContext context, String statusMessage, AtomicBoolean firstFrameSent, long providerRecvTime) {
        LlmHubResponse response = new LlmHubResponse();
        response.statusMessage = statusMessage;
        if (providerRecvTime <= 0) {
            // might be timeout
            providerRecvTime = System.currentTimeMillis();
        }
        response.trace = buildTrace(context, firstFrameSent, providerRecvTime);
        return JsonUtil.writeValueAsString(response);
    }
}
