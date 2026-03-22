package dev.scout.recon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScmReviewRequest {
    @JsonProperty("commit_id")
    private String commitId;

    private String body;
    private String event;

    private List<ScmReviewComment> comments;
}
