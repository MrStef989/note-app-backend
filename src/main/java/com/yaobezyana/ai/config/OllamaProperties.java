package com.yaobezyana.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3.2:3b";
    private boolean enabled = true;
}
