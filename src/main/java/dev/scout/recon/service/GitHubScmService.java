package dev.scout.recon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.scout.recon.model.ReviewComment;
import dev.scout.recon.model.ReviewResponse;
import dev.scout.recon.model.ScmReviewComment;
import dev.scout.recon.model.ScmReviewRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitHubScmService implements ScmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubScmService(
            @Value("${github.token:}") String githubToken,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .build();
    }

    @Override
    public String fetchPullRequestDiff(String owner, String repo, int prNumber) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve()
                .body(String.class);
    }

    @Override
    public void postReview(String owner, String repo, int prNumber, ReviewResponse review) {
        String commitId = fetchLatestCommitId(owner, repo, prNumber);
        String event = determineEvent(review);
        List<ScmReviewComment> inlineComments = buildInlineComments(review.getComments());

        ScmReviewRequest reviewRequest = ScmReviewRequest.builder()
                .commitId(commitId)
                .body(formatSummary(review))
                .event(event)
                .comments(inlineComments)
                .build();

        try {
            restClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews", owner, repo, prNumber)
                    .body(reviewRequest)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully posted review to {}/{} PR#{}", owner, repo, prNumber);
        } catch (Exception e) {
            log.error("Failed to post review to GitHub", e);
            throw new RuntimeException("Failed to post review to GitHub: " + e.getMessage(), e);
        }
    }

    private String fetchLatestCommitId(String owner, String repo, int prNumber) {
        String response = restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.at("/head/sha").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch latest commit SHA", e);
        }
    }

    private String determineEvent(ReviewResponse review) {
        boolean hasCritical = review.getComments().stream()
                .anyMatch(c -> "critical".equalsIgnoreCase(c.getSeverity()));
        return hasCritical ? "REQUEST_CHANGES" : "COMMENT";
    }

    private List<ScmReviewComment> buildInlineComments(List<ReviewComment> comments) {
        return comments.stream()
                .filter(c -> c.getFile() != null && c.getLine() != null)
                .map(c -> ScmReviewComment.builder()
                        .path(c.getFile())
                        .line(c.getLine())
                        .side("RIGHT")
                        .body(formatComment(c))
                        .build())
                .collect(Collectors.toList());
    }

    private String formatComment(ReviewComment comment) {
        return String.format("""
                **[%s]** `%s`
                
                %s
                
                💡 **Suggestion:** %s
                """,
                comment.getSeverity().toUpperCase(),
                comment.getCategory(),
                comment.getIssue(),
                comment.getSuggestion()
        );
    }

    private String formatSummary(ReviewResponse review) {
        long critical = review.getComments().stream()
                .filter(c -> "critical".equalsIgnoreCase(c.getSeverity())).count();
        long warnings = review.getComments().stream()
                .filter(c -> "warning".equalsIgnoreCase(c.getSeverity())).count();
        long suggestions = review.getComments().stream()
                .filter(c -> "suggestion".equalsIgnoreCase(c.getSeverity())).count();

        return String.format("""
                ## 🤖 AI Code Review
                
                %s
                
                ---
                **Summary:** 🔴 %d critical &nbsp;|&nbsp; ⚠️ %d warnings &nbsp;|&nbsp; 💡 %d suggestions
                
                *Reviewed by %s*
                """,
                review.getSummary(),
                critical, warnings, suggestions,
                review.getModel()
        );
    }
}