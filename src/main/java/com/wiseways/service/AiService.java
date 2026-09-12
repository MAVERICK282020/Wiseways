package com.wiseways.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls the NVIDIA / OpenAI-compatible chat-completions endpoint.
 *
 * Python equivalent:
 * <pre>
 *   client = OpenAI(base_url="https://integrate.api.nvidia.com/v1", api_key="...")
 *   completion = client.chat.completions.create(
 *       model="meta/llama-3.1-8b-instruct",
 *       messages=[{"role": "user", "content": query}],
 *       temperature=0.5
 *   )
 *   return completion.choices[0].message.content
 * </pre>
 */
@Slf4j
@Service
public class AiService {

    @Value("${nvidia.api.base-url}")
    private String baseUrl;

    @Value("${nvidia.api.key}")
    private String apiKey;

    @Value("${nvidia.api.model}")
    private String model;

    private final RestTemplate restTemplate;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** True when an API key is configured; otherwise /ask runs in FAQ-only mode. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends {@code query} to the LLM and returns the text reply.
     *
     * @param query  user's question
     * @return       model's response text
     * @throws RuntimeException on HTTP or parse errors (caught by the controller)
     */
    @SuppressWarnings("unchecked")
    public String ask(String query) {
        String url = baseUrl + "/chat/completions";

        // ── Build HTTP headers ────────────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);          // Authorization: Bearer <key>

        // ── Build request payload — same shape as the OpenAI SDK uses ─────────
        Map<String, Object> body = Map.of(
                "model",       model,
                "messages",    List.of(Map.of("role", "user", "content", query)),
                "temperature", 0.5
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null)
            throw new RuntimeException("Non-OK response from AI API: " + response.getStatusCode());

        // Navigate: response → choices[0] → message → content
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");

        if (choices == null || choices.isEmpty())
            throw new RuntimeException("Empty choices array in AI response");

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null)
            throw new RuntimeException("Missing message object in AI response");

        return (String) message.get("content");
    }
}
