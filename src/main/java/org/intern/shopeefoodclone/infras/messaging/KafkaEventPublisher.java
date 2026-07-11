package org.intern.shopeefoodclone.infras.messaging;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.config.kafka.KafkaTopicConfig;
import org.intern.shopeefoodclone.events.OtpVerificationRequestedEvent;
import org.intern.shopeefoodclone.events.UserRegisteredEvent;
import org.intern.shopeefoodclone.user.User;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Domain-facing event publisher that wraps {@link KafkaTemplate}.
 *
 * <p>Domain services should call this class rather than KafkaTemplate directly.
 * This keeps domain code free of Kafka-specific details and makes it easier to
 * swap the messaging infrastructure in tests or future migrations.</p>
 *
 * <p>Each publish method:
 * <ul>
 *   <li>Builds a versioned event POJO with a correlation ID for traceability.</li>
 *   <li>Uses the email as the Kafka message key — guarantees ordering for all events
 *       related to the same user lands in the same partition.</li>
 *   <li>Registers async callbacks for success/failure logging without blocking the caller.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class KafkaEventPublisher {

    KafkaTemplate<String, Object> kafkaTemplate;

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Publishes an {@link OtpVerificationRequestedEvent} to the Kafka topic.
     * Called by {@code UserOtpService} after saving the OTP to Redis/DB.
     *
     * @param email   the recipient's email address (also used as partition key)
     * @param otpCode the 6-digit OTP code
     */
    public void publishOtpVerificationRequested(String email, String otpCode) {
        UUID correlationId = UUID.randomUUID();

        OtpVerificationRequestedEvent event = OtpVerificationRequestedEvent.builder()
                .correlationId(correlationId)
                .email(email)
                .otpCode(otpCode)
                .requestedAt(OffsetDateTime.now())
                .build();

        log.info("[Kafka] Publishing OtpVerificationRequestedEvent | correlationId={} | to={}", correlationId, email);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED, email, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] FAILED to publish OtpVerificationRequestedEvent | correlationId={} | email={} | error={}",
                        correlationId, email, ex.getMessage(), ex);
            } else {
                log.debug("[Kafka] OtpVerificationRequestedEvent sent | correlationId={} | partition={} | offset={}",
                        correlationId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publishes a {@link UserRegisteredEvent} to the Kafka topic.
     * Called by {@code AuthService} after a new user account is created.
     *
     * @param user the newly registered {@link User} entity
     */
    public void publishUserRegistered(User user) {
        UUID correlationId = UUID.randomUUID();

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .correlationId(correlationId)
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getName())
                .registeredAt(OffsetDateTime.now())
                .build();

        log.info("[Kafka] Publishing UserRegisteredEvent | correlationId={} | userId={} | email={}",
                correlationId, user.getId(), user.getEmail());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopicConfig.TOPIC_USER_REGISTERED, user.getEmail(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[Kafka] FAILED to publish UserRegisteredEvent | correlationId={} | userId={} | error={}",
                        correlationId, user.getId(), ex.getMessage(), ex);
            } else {
                log.debug("[Kafka] UserRegisteredEvent sent | correlationId={} | partition={} | offset={}",
                        correlationId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
