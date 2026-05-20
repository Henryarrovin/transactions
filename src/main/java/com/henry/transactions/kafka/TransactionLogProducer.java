package com.henry.transactions.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionLogProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.transaction-logs}")
    private String logTopic;

    public void publishLog(String level, String message, Map<String, Object> context) {
        try {
            var logEntry = Map.of(
                    "level", level,
                    "message", message,
                    "service", "transaction-service",
                    "context", context,
                    "timestamp", System.currentTimeMillis()
            );
            String key = LocalDate.now().toString();
            String value = objectMapper.writeValueAsString(logEntry);
            kafkaTemplate.send(logTopic, key, value);
        } catch (Exception e) {
            log.error("Failed to publish log to Kafka", e);
        }
    }

    public void publishTransactionEvent(String eventType, Map<String, Object> data) {
        try {
            var event = Map.of(
                    "event", eventType,
                    "service", "transaction-service",
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            );
            String value = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(logTopic, eventType, value);
        } catch (Exception e) {
            log.error("Failed to publish transaction event", e);
        }
    }

}
