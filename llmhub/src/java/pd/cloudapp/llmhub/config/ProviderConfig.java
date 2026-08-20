package pd.cloudapp.llmhub.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

    @Bean
    @ConfigurationProperties(prefix = "llmhub.provider")
    public Map<String, Provider> providers() {
        return new LinkedHashMap<>();
    }
}
