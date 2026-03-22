package dev.scout.recon.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are an expert code reviewer. Analyze the provided code diff and review it for:
            - Code quality & best practices
            - Security vulnerabilities
            - Performance issues
            - Bug detection
            - Test coverage suggestions
            
            Always respond in the following JSON format:
            {
              "summary": "overall review summary here",
              "comments": [
                {
                  "severity": "critical|warning|suggestion",
                  "category": "security|performance|quality|bug|test",
                  "file": "filename or null",
                  "line": line_number_or_null,
                  "issue": "description of the issue",
                  "suggestion": "how to fix it"
                }
              ]
            }
            Return only valid JSON, no markdown, no extra text.
            """;

    @Bean
    public ChatClient reviewChatClient(
            @Value("${app.ai.active-model}") String activeModel,
            @Qualifier("googleGenAiChatModel") ChatModel geminiModel,
            @Qualifier("anthropicChatModel") ChatModel anthropicModel,
            @Qualifier("openAiChatModel") ChatModel openAiModel) {

        ChatModel selectedModel = switch (activeModel.toLowerCase()) {
            case "anthropic" -> anthropicModel;
            case "openai" -> openAiModel;
            default -> geminiModel;
        };

        return ChatClient.builder(selectedModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}