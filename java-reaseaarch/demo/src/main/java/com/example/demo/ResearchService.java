package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class ResearchService {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ResearchService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest request) {
        String prompt = buildPrompt(request);

        // Gemini interactions API format:
        // { "model": "gemini-3.5-flash", "input": "YOUR_PROMPT" }
        Map<String, Object> requestBody = Map.of(
                "model", "gemini-3.5-flash",
                "input", prompt
        );

        String response = webClient.post()
                .uri(geminiApiUrl)
                .header("x-goog-api-key", geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractTextFromResponse(response);
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();

        switch (request.getOperation().toLowerCase()) {
            case "summarize":
                prompt.append("Provide a clear and concise summary of the following text in a few sentences: ");
                break;
            case "suggest":
                prompt.append("Based on the following content suggest related topics and further reading. Format the response with clear headings and bullet points: ");
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + request.getOperation());
        }

        prompt.append(request.getContent());
        return prompt.toString();
    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);

            if (geminiResponse.steps != null && !geminiResponse.steps.isEmpty()) {
                for (GeminiResponse.Step step : geminiResponse.steps) {
                    // Skip "thought" steps; we only want the model's answer
                    if ("model_output".equals(step.type)
                            && step.content != null && !step.content.isEmpty()) {

                        StringBuilder sb = new StringBuilder();
                        for (GeminiResponse.ContentPart part : step.content) {
                            if ("text".equals(part.type) && part.text != null) {
                                sb.append(part.text);
                            }
                        }
                        if (!sb.isEmpty()) {
                            return sb.toString();
                        }
                    }
                }
            }
            return "No content found in response";
        } catch (Exception e) {
            return "Error parsing: " + e.getMessage();
        }
    }
}
