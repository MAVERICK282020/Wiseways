package com.wiseways.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a shared {@link RestTemplate} bean used by {@code AiService}
 * to call the NVIDIA / OpenAI-compatible API.
 *
 * Python equivalent: the OpenAI SDK client object.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
