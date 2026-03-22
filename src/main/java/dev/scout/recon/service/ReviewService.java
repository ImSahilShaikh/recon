package dev.scout.recon.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.scout.recon.config.AppProperties;
import dev.scout.recon.model.ReviewComment;
import dev.scout.recon.model.ReviewRequest;
import dev.scout.recon.model.ReviewResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ChatClient reviewChatClient;
    private final ScmService scmService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public ReviewResponse review(ReviewRequest request) throws Exception {
        String diff = resolveDiff(request);

        String rawResponse = reviewChatClient.prompt()
                .user(u -> u.text("""
                        Please review the following code diff:
                        
                        {diff}
                        """).param("diff", diff))
                .call()
                .content();

        ReviewResponse review = parseResponse(rawResponse, appProperties.getActiveModel());

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
        throw new IllegalArgumentException("Either diff or GitHub PR details must be provided");
    }

    private ReviewResponse parseResponse(String raw, String model) throws Exception {
        var json = objectMapper.readTree(raw);
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