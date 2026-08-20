package pd.cloudapp.llmhub.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static byte[] writeValueAsBytes(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize to JSON", e);
        }
    }

    public static String writeValueAsString(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize to JSON", e);
        }
    }

    public static JsonNode readTree(String content) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(content);
    }
}
