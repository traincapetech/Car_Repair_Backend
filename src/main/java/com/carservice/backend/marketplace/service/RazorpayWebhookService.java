package com.carservice.backend.marketplace.service;

import com.carservice.backend.marketplace.entity.Refund;
import com.carservice.backend.marketplace.entity.WorkshopPayment;
import com.carservice.backend.marketplace.enums.MarketplaceEventType;
import com.carservice.backend.marketplace.enums.PaymentStatus;
import com.carservice.backend.marketplace.enums.RefundStatus;
import com.carservice.backend.marketplace.payment.PaymentGateway;
import com.carservice.backend.marketplace.repository.RefundRepository;
import com.carservice.backend.marketplace.repository.WorkshopPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RazorpayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookService.class);

    private final PaymentGateway paymentGateway;
    private final WorkshopPaymentRepository workshopPaymentRepository;
    private final RefundRepository refundRepository;
    private final MarketplaceAuditService auditService;
    private final ObjectMapper objectMapper;

    public RazorpayWebhookService(
            PaymentGateway paymentGateway,
            WorkshopPaymentRepository workshopPaymentRepository,
            RefundRepository refundRepository,
            MarketplaceAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.paymentGateway = paymentGateway;
        this.workshopPaymentRepository = workshopPaymentRepository;
        this.refundRepository = refundRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            log.warn("Rejected webhook: missing X-Razorpay-Signature header");
            throw new IllegalArgumentException("Invalid webhook signature: missing signature header");
        }

        boolean isValid = paymentGateway.verifyWebhookSignature(payload, signature);
        if (!isValid) {
            log.warn("Rejected webhook: signature verification failed");
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.has("event") ? root.get("event").asText() : "";
            log.info("Processing verified Razorpay webhook event: {}", event);

            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null) {
                log.info("Webhook contains no payload node, skipping");
                return;
            }

            switch (event) {
                case "refund.processed":
                    handleRefundProcessed(payloadNode);
                    break;
                case "refund.failed":
                    handleRefundFailed(payloadNode);
                    break;
                case "payment.failed":
                    handlePaymentFailed(payloadNode);
                    break;
                case "payment.captured":
                    handlePaymentCaptured(payloadNode);
                    break;
                default:
                    log.info("Unhandled or informational Razorpay webhook event: {}", event);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing or processing Razorpay webhook: {}", e.getMessage(), e);
            throw new RuntimeException("Webhook processing error: " + e.getMessage(), e);
        }
    }

    private void handleRefundProcessed(JsonNode payloadNode) {
        JsonNode refundEntity = payloadNode.path("refund").path("entity");
        if (refundEntity.isMissingNode()) {
            return;
        }

        String refundId = refundEntity.path("id").asText(null);
        String paymentId = refundEntity.path("payment_id").asText(null);

        Optional<Refund> refundOpt = Optional.empty();
        if (refundId != null && !refundId.isBlank()) {
            refundOpt = refundRepository.findByRazorpayRefundId(refundId);
        }
        if (refundOpt.isEmpty() && paymentId != null && !paymentId.isBlank()) {
            Optional<WorkshopPayment> paymentOpt = workshopPaymentRepository.findByRazorpayPaymentId(paymentId);
            if (paymentOpt.isPresent()) {
                var list = refundRepository.findByPaymentId(paymentOpt.get().getId());
                if (!list.isEmpty()) {
                    refundOpt = Optional.of(list.get(0));
                }
            }
        }

        if (refundOpt.isPresent()) {
            Refund refund = refundOpt.get();
            if (refund.getRefundStatus() == RefundStatus.SUCCESS) {
                log.info("Idempotent webhook skip: refund #{} already marked SUCCESS", refund.getId());
                return;
            }

            refund.setRefundStatus(RefundStatus.SUCCESS);
            refund.setProcessedAt(LocalDateTime.now());
            if (refundId != null && (refund.getRazorpayRefundId() == null || refund.getRazorpayRefundId().isBlank())) {
                refund.setRazorpayRefundId(refundId);
            }
            refundRepository.save(refund);

            WorkshopPayment payment = refund.getPayment();
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            workshopPaymentRepository.save(payment);

            auditService.recordEvent(
                    MarketplaceEventType.REFUND_SUCCESS,
                    refund.getOpportunity().getServiceRequest().getId(),
                    refund.getOpportunity().getId(),
                    refund.getWorkshop().getId(),
                    null,
                    "Webhook confirmed Razorpay refund successful for payment #" + payment.getId() + " (" + refundId + ")",
                    "{\"source\": \"WEBHOOK\", \"razorpayRefundId\": \"" + refundId + "\"}"
            );
        } else {
            log.warn("Webhook refund.processed could not find matching refund record for refundId={}, paymentId={}", refundId, paymentId);
        }
    }

    private void handleRefundFailed(JsonNode payloadNode) {
        JsonNode refundEntity = payloadNode.path("refund").path("entity");
        if (refundEntity.isMissingNode()) {
            return;
        }

        String refundId = refundEntity.path("id").asText(null);
        String paymentId = refundEntity.path("payment_id").asText(null);
        String errorDescription = refundEntity.path("error_description").asText("Razorpay refund failed");

        Optional<Refund> refundOpt = Optional.empty();
        if (refundId != null && !refundId.isBlank()) {
            refundOpt = refundRepository.findByRazorpayRefundId(refundId);
        }
        if (refundOpt.isEmpty() && paymentId != null && !paymentId.isBlank()) {
            Optional<WorkshopPayment> paymentOpt = workshopPaymentRepository.findByRazorpayPaymentId(paymentId);
            if (paymentOpt.isPresent()) {
                var list = refundRepository.findByPaymentId(paymentOpt.get().getId());
                if (!list.isEmpty()) {
                    refundOpt = Optional.of(list.get(0));
                }
            }
        }

        if (refundOpt.isPresent()) {
            Refund refund = refundOpt.get();
            if (refund.getRefundStatus() == RefundStatus.FAILED) {
                log.info("Idempotent webhook skip: refund #{} already marked FAILED", refund.getId());
                return;
            }

            refund.setRefundStatus(RefundStatus.FAILED);
            refund.setFailureReason(errorDescription);
            refundRepository.save(refund);

            WorkshopPayment payment = refund.getPayment();
            payment.setPaymentStatus(PaymentStatus.REFUND_FAILED);
            payment.setFailureReason(errorDescription);
            workshopPaymentRepository.save(payment);

            auditService.recordEvent(
                    MarketplaceEventType.REFUND_FAILED,
                    refund.getOpportunity().getServiceRequest().getId(),
                    refund.getOpportunity().getId(),
                    refund.getWorkshop().getId(),
                    null,
                    "Webhook confirmed Razorpay refund failure for payment #" + payment.getId() + ": " + errorDescription,
                    "{\"source\": \"WEBHOOK\", \"error\": \"" + errorDescription + "\"}"
            );
        }
    }

    private void handlePaymentFailed(JsonNode payloadNode) {
        JsonNode paymentEntity = payloadNode.path("payment").path("entity");
        if (paymentEntity.isMissingNode()) {
            return;
        }

        String orderId = paymentEntity.path("order_id").asText(null);
        String paymentId = paymentEntity.path("id").asText(null);
        String errorDesc = paymentEntity.path("error_description").asText("Gateway payment failed");

        Optional<WorkshopPayment> paymentOpt = Optional.empty();
        if (orderId != null && !orderId.isBlank()) {
            paymentOpt = workshopPaymentRepository.findByRazorpayOrderId(orderId);
        }
        if (paymentOpt.isEmpty() && paymentId != null && !paymentId.isBlank()) {
            paymentOpt = workshopPaymentRepository.findByRazorpayPaymentId(paymentId);
        }

        if (paymentOpt.isPresent()) {
            WorkshopPayment payment = paymentOpt.get();
            if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
                log.info("Idempotent webhook skip: payment #{} already marked FAILED", payment.getId());
                return;
            }

            // Only update if not already won or refunded
            if (payment.getPaymentStatus() == PaymentStatus.CREATED || payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(errorDesc);
                if (paymentId != null && (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank())) {
                    payment.setRazorpayPaymentId(paymentId);
                }
                workshopPaymentRepository.save(payment);

                auditService.recordEvent(
                        MarketplaceEventType.PAYMENT_FAILED,
                        payment.getOpportunity().getServiceRequest().getId(),
                        payment.getOpportunity().getId(),
                        payment.getWorkshop().getId(),
                        null,
                        "Webhook confirmed payment #" + payment.getId() + " failed: " + errorDesc,
                        "{\"source\": \"WEBHOOK\", \"error\": \"" + errorDesc + "\"}"
                );
            }
        }
    }

    private void handlePaymentCaptured(JsonNode payloadNode) {
        JsonNode paymentEntity = payloadNode.path("payment").path("entity");
        if (paymentEntity.isMissingNode()) {
            return;
        }

        String orderId = paymentEntity.path("order_id").asText(null);
        String paymentId = paymentEntity.path("id").asText(null);

        if (orderId != null && !orderId.isBlank()) {
            Optional<WorkshopPayment> paymentOpt = workshopPaymentRepository.findByRazorpayOrderId(orderId);
            if (paymentOpt.isPresent()) {
                WorkshopPayment payment = paymentOpt.get();
                if (paymentId != null && (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank())) {
                    payment.setRazorpayPaymentId(paymentId);
                    workshopPaymentRepository.save(payment);
                }
                // CRITICAL: Webhook NEVER unlocks customer details or bypasses atomic claim.
                log.info("Webhook recorded capture for payment #{} (order: {}) - awaiting client verification for atomic claim", payment.getId(), orderId);
            }
        }
    }
}
