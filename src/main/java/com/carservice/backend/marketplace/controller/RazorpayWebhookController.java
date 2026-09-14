package com.carservice.backend.marketplace.controller;

import com.carservice.backend.common.response.ApiResponse;
import com.carservice.backend.marketplace.service.RazorpayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    public RazorpayWebhookController(RazorpayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping({"/api/v1/payments/razorpay/webhook", "/api/v1/partner/payments/razorpay/webhook"})
    public ResponseEntity<ApiResponse<Void>> handleRazorpayWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload
    ) {
        webhookService.processWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", null));
    }
}
