package pd.cloudapp.llmhub.service;

import java.time.Duration;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pd.cloudapp.llmhub.config.LlmHubProperties;
import pd.cloudapp.llmhub.util.JsonUtil;
import reactor.core.publisher.Mono;

/**
 * Pre-establishes a pooled TCP/TLS connection to each configured provider on startup, so the
 * first real request doesn't pay that ~hundreds-of-ms connect cost on the hot path.
 */
@Component
@RequiredArgsConstructor
public class ConnectionWarmer {

    private static final Logger log = LogManager.getLogger(ConnectionWarmer.class);

    private final WebClient upstreamWebClient;
    private final LlmHubProperties llmHubProperties;

    @Value("${server.port}")
    private int serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        // forces JIT/class-loading for Jackson + Log4j2
        JsonUtil.writeValueAsBytes(Collections.singletonMap("warmup", true));
        log.info("llmhub logging warm-up done");

        warmUpInboundPipeline();

        if (llmHubProperties.getProviders() == null) {
            return;
        }
        llmHubProperties.getProviders().forEach((name, provider) -> {
            if (provider.getEndpoint() == null || provider.getEndpoint().isEmpty()) {
                return;
            }
            // response status/body is irrelevant, only the connect+TLS handshake matters here
            upstreamWebClient.method(HttpMethod.GET)
                    .uri(provider.getEndpoint())
                    .exchangeToMono(response -> response.releaseBody())
                    .timeout(Duration.ofSeconds(5))
                    .doOnSuccess(v -> log.info("llmhub connection warm-up done: provider={}", name))
                    .onErrorResume(e -> {
                        log.warn("llmhub connection warm-up failed (harmless): provider={}, error={}", name, e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        });
    }

    private void warmUpInboundPipeline() {
        // only exercises Netty + WebFilter chain + Jackson @RequestBody decode + routing + error handling
        // exchangeToFlux (rather than retrieve()) treats the expected 400 response as a normal signal, not an error
        upstreamWebClient.post()
                .uri("http://localhost:" + serverPort + "/api/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue("{}")
                .exchangeToFlux(response -> response.bodyToFlux(String.class))
                .then()
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(v -> log.info("llmhub inbound pipeline warm-up done"))
                .onErrorResume(e -> {
                    log.warn("llmhub inbound pipeline warm-up failed (harmless): error={}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
