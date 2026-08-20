package pd.cloudapp.llmhub.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import pd.cloudapp.llmhub.entity.RequestContext;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient upstreamWebClient(LlmHubProperties properties) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("llmhub-upstream")
                .maxConnections(properties.getMaxConnections())
                // bounded queue + timeout so callers fail fast instead of piling up when the pool is exhausted
                .pendingAcquireMaxCount(properties.getPendingAcquireMaxCount())
                .pendingAcquireTimeout(Duration.ofMillis(properties.getPendingAcquireTimeoutMs()))
                .maxIdleTime(Duration.ofSeconds(60))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .option(ChannelOption.TCP_NODELAY, true)
                // compression would buffer chunks and add CPU/latency on the token streaming hot path
                .compress(false)
                // fires right as the request is written to the connection, i.e. the real wire send time
                .doOnRequest((req, conn) -> {
                    RequestContext context = req.currentContextView().getOrDefault(RequestContext.class, null);
                    if (context != null) {
                        context.providerSendTime = System.currentTimeMillis();
                    }
                });
        if (properties.getResponseTimeoutMs() > 0) {
            httpClient = httpClient.responseTimeout(Duration.ofMillis(properties.getResponseTimeoutMs()));
        }

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(properties.getMaxInMemorySizeBytes()))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
