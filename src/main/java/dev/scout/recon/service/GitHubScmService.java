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

import java.util.ArrayList;
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

        log.info("Attempting to post review with {} inline comments", inlineComments.size());

        try {
            // Step 1: Try to post the review as a batch (most efficient and clean)
            submitReviewBatch(owner, repo, prNumber, commitId, formatSummary(review), event, inlineComments);
            log.info("Successfully posted batch review to {}/{} PR#{}", owner, repo, prNumber);
        } catch (Exception e) {
            log.warn("Batch review failed: {}. Attempting individual comment posting...", e.getMessage());

            // Step 2: Post the summary first
            try {
                submitReviewBatch(owner, repo, prNumber, commitId, formatSummary(review), "COMMENT", new ArrayList<>());
                log.info("Summary posted. Now trying individual comments...");
            } catch (Exception e2) {
                log.error("Failed to post summary: {}", e2.getMessage());
            }

            // Step 3: Try to post each comment individually
            for (ScmReviewComment comment : inlineComments) {
                try {
                    postIndividualComment(owner, repo, prNumber, commitId, comment);
                    log.info("Successfully posted individual comment for {} at line {}", comment.getPath(), comment.getLine());
                } catch (Exception e3) {
                    log.error("Failed to post individual comment for {} at line {}: {}", 
                            comment.getPath(), comment.getLine(), e3.getMessage());
                }
            }
        }
    }

    private void submitReviewBatch(String owner, String repo, int prNumber, String commitId, String summary, String event, List<ScmReviewComment> comments) {
        ScmReviewRequest request = ScmReviewRequest.builder()
                .commitId(commitId)
                .body(summary)
                .event(event)
                .comments(comments != null ? comments : new ArrayList<>())
                .build();

        restClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews", owner, repo, prNumber)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void postIndividualComment(String owner, String repo, int prNumber, String commitId, ScmReviewComment comment) {
        // GitHub uses a slightly different model for individual PR comments vs batch review comments
        // But for most fields it's similar. We need to include the commit_id.
        var body = new java.util.HashMap<String, Object>();
        body.put("body", comment.getBody());
        body.put("commit_id", commitId);
        body.put("path", comment.getPath());
        body.put("line", comment.getLine());
        body.put("side", comment.getSide());

        restClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/comments", owner, repo, prNumber)
                .body(body)
                .retrieve()
                .toBodilessEntity();
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
                .filter(c -> c.getFile() != null && c.getLine() != null && c.getLine() > 0)
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
