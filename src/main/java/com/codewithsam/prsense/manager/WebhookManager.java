package com.codewithsam.prsense.manager;

import com.codewithsam.prsense.dto.request.WebhookPayloadRequest;
import com.codewithsam.prsense.dto.response.ReviewResponse;

// Orchestrates the full PR review flow: validate event → fetch diffs → AI review → post comments
public interface WebhookManager {

    ReviewResponse handlePrWebhook(WebhookPayloadRequest payload);
}
