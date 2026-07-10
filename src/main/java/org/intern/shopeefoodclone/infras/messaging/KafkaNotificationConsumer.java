package org.intern.shopeefoodclone.infras.messaging;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.config.kafka.KafkaTopicConfig;
import org.intern.shopeefoodclone.events.OtpVerificationRequestedEvent;
import org.intern.shopeefoodclone.events.UserRegisteredEvent;
import org.intern.shopeefoodclone.infras.notification.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for notification-related domain events.
 *
 * <p>Each listener method follows the same reliable processing pattern:</p>
 * <ol>
 *   <li>Log incoming event with correlation ID for distributed tracing.</li>
 *   <li>Delegate actual work to the domain service (e.g., {@link EmailService}).</li>
 *   <li>Call {@code ack.acknowledge()} <em>after</em> successful processing to commit the offset.
 *       If an exception is thrown, the offset is NOT committed, and the
 *       {@code DefaultErrorHandler} will retry up to 3 times before publishing
 *       the message to the Dead Letter Topic.</li>
 * </ol>
 *
 * <p>This consumer is intentionally stateless — it translates events into service calls,
 * keeping the notification logic in {@link EmailService} and out of this class.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class KafkaNotificationConsumer {

    EmailService emailService;

    // ── OTP Verification Listener ───────────────────────────────────────────

    /**
     * Consumes {@link OtpVerificationRequestedEvent} and dispatches the OTP email.
     *
     * @param event     the deserialized event payload
     * @param partition the Kafka partition this record came from (for observability)
     * @param offset    the Kafka offset of this record (for observability)
     * @param ack       manual acknowledgment handle — committed only on success
     */
    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOtpVerificationRequested(
            @Payload OtpVerificationRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack
    ) {
        log.info("[Kafka] Received OtpVerificationRequestedEvent | correlationId={} | email={} | partition={} | offset={}",
                event.getCorrelationId(), event.getEmail(), partition, offset);

        try {
            emailService.sendOtpEmail(event.getEmail(), event.getOtpCode());
            ack.acknowledge();
            log.debug("[Kafka] OtpVerificationRequestedEvent processed and offset committed | correlationId={}",
                    event.getCorrelationId());
        } catch (Exception e) {
            log.error("[Kafka] Failed to process OtpVerificationRequestedEvent | correlationId={} | error={}",
                    event.getCorrelationId(), e.getMessage(), e);
            // Re-throw so DefaultErrorHandler can apply retry + DLT logic
            throw e;
        }
    }

    // ── User Registered Listener ────────────────────────────────────────────

    /**
     * Consumes {@link UserRegisteredEvent} and sends a welcome email to the new user.
     *
     * @param event     the deserialized event payload
     * @param partition the Kafka partition this record came from (for observability)
     * @param offset    the Kafka offset of this record (for observability)
     * @param ack       manual acknowledgment handle — committed only on success
     */
    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_USER_REGISTERED,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(
            @Payload UserRegisteredEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack
    ) {
        log.info("[Kafka] Received UserRegisteredEvent | correlationId={} | userId={} | email={} | partition={} | offset={}",
                event.getCorrelationId(), event.getUserId(), event.getEmail(), partition, offset);

        try {
            String subject = "Welcome to ShopeeFood! 🎉";
            String body = String.format(
                    "Hi %s, welcome to ShopeeFood! Your account has been created successfully. " +
                    "Start exploring restaurants near you.",
                    event.getDisplayName()
            );
            emailService.sendEmail(event.getEmail(), subject, body);
            ack.acknowledge();
            log.debug("[Kafka] UserRegisteredEvent processed and offset committed | correlationId={}",
                    event.getCorrelationId());
        } catch (Exception e) {
            log.error("[Kafka] Failed to process UserRegisteredEvent | correlationId={} | error={}",
                    event.getCorrelationId(), e.getMessage(), e);
            throw e;
        }
    }
}
