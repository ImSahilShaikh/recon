package dev.scout.recon.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private String summary;
    private List<ReviewComment> comments;
    private String model;
}
