package dev.scout.recon.model;

import lombok.Data;

@Data
public class ReviewRequest {
    private String githubPrUrl;
    private String diff;
    private String repoOwner;
    private String repoName;
    private Integer prNumber;
    private String model;
}
