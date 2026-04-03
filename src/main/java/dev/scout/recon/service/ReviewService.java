package dev.scout.recon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.scout.recon.config.AppProperties;
import dev.scout.recon.model.ReviewComment;
import dev.scout.recon.model.ReviewRequest;
import dev.scout.recon.model.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ChatClient reviewChatClient;
    private final ScmService scmService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    private static final Pattern GITHUB_PR_PATTERN = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)/pull/(\\d+)");

    public ReviewResponse review(ReviewRequest request) throws Exception {
        resolvePrDetails(request);
        String diff = resolveDiff(request);

        log.info("Sending diff to AI for review...");
        String rawResponse = reviewChatClient.prompt()
                .user(u -> u.text("""
                        Please review the following code diff:
                        
                        {diff}
                        """).param("diff", diff))
                .call()
                .content();

        if (rawResponse == null) {
            throw new RuntimeException("AI response is null");
        }

        log.debug("Raw AI Response: {}", rawResponse);

        ReviewResponse review = parseResponse(rawResponse, appProperties.getActiveModel());
        log.info("AI generated {} comments", review.getComments().size());

        if (request.getRepoOwner() != null && request.getRepoName() != null && request.getPrNumber() != null) {
            scmService.postReview(
                    request.getRepoOwner(),
                    request.getRepoName(),
                    request.getPrNumber(),
                    review
            );
        }

        return review;
    }

    private void resolvePrDetails(ReviewRequest request) {
        if (request.getGithubPrUrl() != null && !request.getGithubPrUrl().isBlank()) {
            Matcher matcher = GITHUB_PR_PATTERN.matcher(request.getGithubPrUrl());
            if (matcher.find()) {
                request.setRepoOwner(matcher.group(1));
                request.setRepoName(matcher.group(2));
                request.setPrNumber(Integer.parseInt(matcher.group(3)));
            } else {
                throw new IllegalArgumentException("Invalid GitHub PR URL format");
            }
        }
    }

    private String resolveDiff(ReviewRequest request) {
        if (request.getDiff() != null && !request.getDiff().isBlank()) {
            return request.getDiff();
        }
        if (request.getRepoOwner() != null && request.getRepoName() != null && request.getPrNumber() != null) {
            return scmService.fetchPullRequestDiff(
                    request.getRepoOwner(),
                    request.getRepoName(),
                    request.getPrNumber()
            );
        }
        throw new IllegalArgumentException("Either diff or a valid GitHub PR URL must be provided");
    }

    private ReviewResponse parseResponse(String raw, String model) throws Exception {
        String cleanJson = raw.trim();
        
        // Remove markdown formatting if present
        if (cleanJson.contains("```json")) {
            cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
            cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```")).trim();
        } else if (cleanJson.contains("```")) {
            cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3);
            cleanJson = cleanJson.substring(0, cleanJson.lastIndexOf("```")).trim();
        }

        var json = objectMapper.readTree(cleanJson);
        String summary = json.get("summary").asText();

        List<ReviewComment> comments = objectMapper.convertValue(
                json.get("comments"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ReviewComment.class)
        );

        return ReviewResponse.builder()
                .summary(summary)
                .comments(comments)
                .model(model)
                .build();
    }
}
