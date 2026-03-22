package dev.scout.recon.controller;

import dev.scout.recon.model.ReviewRequest;
import dev.scout.recon.model.ReviewResponse;
import dev.scout.recon.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> review(@RequestBody ReviewRequest request) throws Exception {
        return ResponseEntity.ok(reviewService.review(request));
    }
}