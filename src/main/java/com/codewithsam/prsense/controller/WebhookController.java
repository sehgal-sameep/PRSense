package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.config.WebhookProperties;
import com.codewithsam.prsense.dto.request.ReviewRequest;
import com.codewithsam.prsense.dto.request.TriggerType;
import com.codewithsam.prsense.dto.request.WebhookPayloadRequest;
import com.codewithsam.prsense.dto.response.ReviewResponse;
import com.codewithsam.prsense.manager.ReviewManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/webhook")
@Slf4j
@Tag(name = "Webhook APIs", description = "APIs for handling Azure DevOps PR webhook events")
public class WebhookController {

    private static final Set<String> REVIEWABLE_EVENTS = Set.of(
            "git.pullrequest.created",
            "git.pullrequest.updated"
    );

    private final ReviewManager reviewManager;
    private final WebhookProperties webhookProperties;

    public WebhookController(ReviewManager reviewManager, WebhookProperties webhookProperties) {
        this.reviewManager = reviewManager;
        this.webhookProperties = webhookProperties;
    }

    @PostMapping("/pr-sense")
    @Operation(
            summary = "Handle PR webhook",
            description = "Receives Azure DevOps pull request events and triggers AI code review asynchronously"
    )
    public ResponseEntity<ReviewResponse> handlePrWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody WebhookPayloadRequest payload) {

        if (!webhookProperties.getSecret().equals(secret)) {
            log.warn("Rejected webhook — invalid or missing X-Webhook-Secret header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ReviewResponse.builder().status("unauthorized").build());
        }

        if (payload == null || payload.getResource() == null
                || payload.getResource().getRepository() == null) {
            log.warn("Rejected webhook — malformed payload");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ReviewResponse.builder().status("bad_request").build());
        }

        String eventType = payload.getEventType();
        if (!REVIEWABLE_EVENTS.contains(eventType)) {
            log.info("Ignoring unsupported event type: {}", eventType);
            return ResponseEntity.ok(ReviewResponse.builder().status("ignored").build());
        }

        int prId = payload.getResource().getPullRequestId();
        String repoId = payload.getResource().getRepository().getId();
        String project = payload.getResource().getRepository().getProject().getName();

        log.info("Accepted webhook — eventType: {}, PR #{}", eventType, prId);

        ReviewRequest reviewRequest = ReviewRequest.builder()
                .triggerType(TriggerType.WEBHOOK)
                .repositoryId(repoId)
                .pullRequestId(prId)
                .projectName(project)
                .build();

        String reviewId = reviewManager.processReview(reviewRequest);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ReviewResponse.builder()
                        .reviewId(reviewId)
                        .status("queued")
                        .prId(prId)
                        .build());
    }
}
