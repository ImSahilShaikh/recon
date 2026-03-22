package dev.scout.recon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScmReviewComment {
    private String path;
    private Integer line;

    @JsonProperty("side")
    private String side;

    private String body;
}
