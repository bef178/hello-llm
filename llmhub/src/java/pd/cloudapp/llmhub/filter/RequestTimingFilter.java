package pd.cloudapp.llmhub.filter;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Stamps the request arrival time before body reading/JSON decoding happens, so RequestContext.startTime
 * reflects when the request actually reached the server instead of when the controller method got invoked.
 */
@Component
public class RequestTimingFilter implements WebFilter, Ordered {

    public static final String START_TIME_ATTR = "llmhub.startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
