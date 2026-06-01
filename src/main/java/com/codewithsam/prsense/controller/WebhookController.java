package com.codewithsam.prsense.controller;

import com.codewithsam.prsense.config.WebhookProperties;
import com.codewithsam.prsense.dto.request.WebhookPayloadRequest;
import com.codewithsam.prsense.dto.response.ReviewResponse;
import com.codewithsam.prsense.manager.WebhookManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;

@RestController
@RequestMapping("/webhook")
@Slf4j
@Tag(name = "Webhook APIs", description = "APIs for handling Azure DevOps PR webhook events")
public class WebhookController {

    private final WebhookManager webhookManager;
    private final WebhookProperties webhookProperties;
    private final Executor reviewExecutor;

    // Explicit constructor needed because @Qualifier isn't supported by @RequiredArgsConstructor
    public WebhookController(WebhookManager webhookManager,
                             WebhookProperties webhookProperties,
                             @Qualifier("reviewExecutor") Executor reviewExecutor) {
        this.webhookManager = webhookManager;
        this.webhookProperties = webhookProperties;
        this.reviewExecutor = reviewExecutor;
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

        int prId = payload.getResource().getPullRequestId();
        log.info("Accepted webhook: POST /webhook/pr-sense — eventType: {}, PR #{}",
                payload.getEventType(), prId);

        // Dispatch async so Azure DevOps gets an immediate response (avoids retry storms)
        reviewExecutor.execute(() -> {
            try {
                webhookManager.handlePrWebhook(payload);
            } catch (Exception e) {
                log.error("Unhandled error during async PR review for PR #{}: {}", prId, e.getMessage(), e);
            }
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ReviewResponse.builder().status("accepted").prId(prId).build());
    }
}
