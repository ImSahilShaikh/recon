package dev.scout.recon.service;

import dev.scout.recon.model.ReviewResponse;

public interface ScmService {
    String fetchPullRequestDiff(String owner, String repo, int prNumber);
    void postReview(String owner, String repo, int prNumber, ReviewResponse review);
}
