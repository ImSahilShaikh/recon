package dev.scout.recon.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewComment {
    private String severity;
    private String category;
    private String file;
    private Integer line;
    private String issue;
    private String suggestion;
}