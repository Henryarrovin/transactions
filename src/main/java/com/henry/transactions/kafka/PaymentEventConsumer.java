package com.henry.transactions.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henry.transactions.dto.request.RecordTransactionRequest;
import com.henry.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes payment-events from a payment-gateway Kafka topic.

 * Payment-gateway publishes events after:
 *   - capture → event: payment.captured
 *   - refund → event: refund.processed

 * This consumer records those events as transactions — async backup
 * alongside the direct gRPC call from payment-gateway.

 * Even if gRPC fails, Kafka guarantees eventual consistency.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received payment event: key={} offset={}",
                record.key(), record.offset());

        try {
            JsonNode payload = objectMapper.readTree(record.value());
            String event = payload.path("event").asText();

            log.info("Processing payment event: {}", event);

            switch (event) {
                case "payment.captured" -> handlePaymentCaptured(payload);
                case "refund.processed"  -> handleRefundProcessed(payload);
                default -> log.info("Ignoring unhandled event type: {}", event);
            }

            // Acknowledge — removes from Kafka queue
            ack.acknowledge();
            log.info("Payment event processed and acknowledged: {}", event);

        } catch (Exception e) {
            log.error("Failed to process payment event: key={} value={}",
                    record.key(), record.value(), e);
            // Don't acknowledge — Kafka will retry
            // In production: implement dead-letter queue after N retries
        }
    }

    // payment.captured

    private void handlePaymentCaptured(JsonNode payload) {
        JsonNode entity = payload.path("payload").path("payment").path("entity");

        if (entity.isMissingNode()) {
            log.warn("payment.captured event missing payment entity");
            return;
        }

        String paymentId  = entity.path("id").asText();
        String orderId    = entity.path("order_id").asText();
        long   amount     = entity.path("amount").asLong();
        String userId     = entity.path("user_id").asText("");

        // user_id may not be in webhook — it's in our order record
        // payment-gateway should include it when publishing to Kafka
        if (userId.isEmpty()) {
            log.warn("payment.captured event missing user_id for payment: {}", paymentId);
            return;
        }

        log.info("Recording payment transaction: paymentId={} amount={}", paymentId, amount);

        var req = new RecordTransactionRequest();
        req.setUserId(userId);
        req.setType("payment");
        req.setAmount(amount);
        req.setCurrency("INR");
        req.setProviderOrderId(orderId);
        req.setProviderPaymentId(paymentId);
        req.setDescription("Payment captured via Kafka event");

        transactionService.recordTransaction(req);
    }

    // refund.processed

    private void handleRefundProcessed(JsonNode payload) {
        JsonNode entity = payload.path("payload").path("refund").path("entity");

        if (entity.isMissingNode()) {
            log.warn("refund.processed event missing refund entity");
            return;
        }

        String refundId   = entity.path("id").asText();
        String paymentId  = entity.path("payment_id").asText();
        long   amount     = entity.path("amount").asLong();
        String userId     = entity.path("user_id").asText("");

        if (userId.isEmpty()) {
            log.warn("refund.processed event missing user_id for refund: {}", refundId);
            return;
        }

        log.info("Recording refund transaction: refundId={} amount={}", refundId, amount);

        var req = new RecordTransactionRequest();
        req.setUserId(userId);
        req.setType("refund");
        req.setAmount(amount);
        req.setCurrency("INR");
        req.setProviderPaymentId(paymentId);
        req.setProviderRefundId(refundId);
        req.setDescription("Refund processed via Kafka event");

        transactionService.recordTransaction(req);
    }
}
