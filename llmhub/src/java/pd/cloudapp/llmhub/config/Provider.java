package pd.cloudapp.llmhub.config;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Provider {

    private String endpoint;

    private String apiKey;

    private Map<String, Object> paramOverride;

    /**
     * [device-id]
     */
    private List<String> deviceWhitelist;
}
