package pd.cloudapp.llmhub.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OpsController {

    @GetMapping("/status")
    public Object status() {
        return "\"You know the service is working by seeing this.\"";
    }
}
