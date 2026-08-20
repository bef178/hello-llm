package pd.cloudapp.llmhub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pd.cloudapp.llmhub.entity.LlmHubRequest;
import pd.cloudapp.llmhub.entity.LlmHubResponse;
import pd.cloudapp.llmhub.entity.RequestContext;
import pd.cloudapp.llmhub.filter.RequestTimingFilter;
import pd.cloudapp.llmhub.service.LlmHubService;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LlmHubController {

    private final LlmHubService llmHubService;

    @PostMapping(value = "/api/dispatch",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> dispatch(@RequestBody LlmHubRequest request,
            ServerWebExchange exchange,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId,
            @RequestHeader(value = "X-Device-ID", required = false) String deviceId) {
        Long startTime = exchange.getAttribute(RequestTimingFilter.START_TIME_ATTR);
        RequestContext context = new RequestContext(request.traceId, startTime != null ? startTime : System.currentTimeMillis());
        context.request = request;
        context.requestId = requestId;
        context.sessionId = sessionId;
        context.deviceId = deviceId;
        return llmHubService.streamDispatch(context);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<LlmHubResponse> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<LlmHubResponse> internalServerError(Exception e) {
        log.error("llmhub request failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("internal error"));
    }

    private LlmHubResponse errorResponse(String message) {
        LlmHubResponse response = new LlmHubResponse();
        response.statusMessage = message;
        return response;
    }
}
