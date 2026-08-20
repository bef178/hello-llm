package pd.cloudapp.llmhub.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "llmhub.provider-router")
public class RouterConfig {

    private String defaultProvider;

    /** provider name -> field name -> value forced into the openai request body before forwarding */
    private Map<String, Map<String, Object>> overrides;

    public Map<String, Object> getOverridesConfig(String providerName) {
        return overrides != null ? overrides.get(providerName) : null;
    }
}
