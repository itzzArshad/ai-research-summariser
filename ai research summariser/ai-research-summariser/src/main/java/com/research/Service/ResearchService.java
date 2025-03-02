package com.research.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.ResearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.research.Service.GeminiResponse;
import org.springframework.web.reactive.function.client.WebClient;

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
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }


    public String processContent(ResearchRequest researchRequest) {
        //build the prompt
        String prompt= buildPrompt(researchRequest);
        //query the AI model api
        Map<String,Object> requestBody=Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                            Map.of("text",prompt)
                })

                }
        );

        String response= webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //parse the response
        //return response
        return extractTextFromResponse(response);

    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
                if (firstCandidate.getContent() != null &&
                        firstCandidate.getContent().getParts() != null &&
                        !firstCandidate.getContent().getParts().isEmpty()) {
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            return "No content found in response";

        } catch (Exception e) {
            return "Error parsing:" + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest researchRequest) {
        StringBuilder prompt = new StringBuilder();
        switch (researchRequest.getOperation()){
            case "summarize" :
                prompt.append("Provide a clear and concise summary of the following text in few sentences :\n\n");
                break;
            case  "suggest":
                prompt.append("Based on the following content : suggest related topics and further reading. Format the response with clear headings and bullet points :\n \n");
                break;
            default:
                throw new IllegalArgumentException("Unknown operation : " + researchRequest.getOperation());
        }
        prompt.append(researchRequest.getContent());
        return prompt.toString();
    }


}
