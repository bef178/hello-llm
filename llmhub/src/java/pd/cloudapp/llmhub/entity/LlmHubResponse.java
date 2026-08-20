package pd.cloudapp.llmhub.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmHubResponse {

    public String statusMessage;

    // written as-is, no reparse
    @JsonRawValue
    public String openai;

    public Map<String, Object> trace;
}
