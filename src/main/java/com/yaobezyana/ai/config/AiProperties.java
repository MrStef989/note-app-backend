package com.yaobezyana.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.groq")
public class AiProperties {
    private String apiKey = "";
    private String model = "llama-3.1-8b-instant";
    private boolean enabled = true;
}
