package pd.cloudapp.llmhub.sse;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

/**
 * Small helpers reused when assembling LlmHubResponse instances: the "[DONE]" sentinel and a way
 * to embed it as a quoted JSON string value into the @JsonRawValue "openai" field.
 */
public final class SseFrameBuilder {

    public static final String DONE_MARKER = "[DONE]";

    private SseFrameBuilder() {
    }

    /** Escapes a string once so per-chunk frame building can reuse the result instead of re-escaping repeatedly. */
    public static String quoteJsonString(String value) {
        StringBuilder sb = new StringBuilder(32);
        appendJsonString(sb, value);
        return sb.toString();
    }

    private static void appendJsonString(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        sb.append('"').append(JsonStringEncoder.getInstance().quoteAsString(value)).append('"');
    }
}

