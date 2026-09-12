package com.wiseways.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates the assistant: scripted FAQ answers first (offline),
 * LLM second (only when an API key is configured), graceful fallback last.
 */
@Slf4j
@Service
public class ChatService {

    private final FaqService faqService;
    private final AiService aiService;

    public ChatService(FaqService faqService, AiService aiService) {
        this.faqService = faqService;
        this.aiService = aiService;
    }

    public Map<String, Object> ask(String query) {
        Map<String, Object> response = new HashMap<>();

        String faq = faqService.answer(query);
        if (faq != null) {
            response.put("source", "faq");
            response.put("answer", faq);
            return response;
        }

        if (aiService.isConfigured()) {
            try {
                response.put("source", "llm");
                response.put("answer", aiService.ask(query));
                return response;
            } catch (Exception e) {
                log.warn("LLM call failed, using fallback: {}", e.getMessage());
            }
        }

        response.put("source", "fallback");
        response.put("answer", "I don't have a scripted answer for that, and the LLM assistant isn't configured on "
                + "this server. Try one of these common questions, or use /recommend for college suggestions:");
        response.put("suggestions", faqService.getSuggestions());
        return response;
    }
}
