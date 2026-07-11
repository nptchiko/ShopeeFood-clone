package org.intern.shopeefoodclone.infras.messaging;

import org.intern.shopeefoodclone.config.kafka.KafkaTopicConfig;
import org.intern.shopeefoodclone.events.OtpVerificationRequestedEvent;
import org.intern.shopeefoodclone.events.UserRegisteredEvent;
import org.intern.shopeefoodclone.infras.notification.EmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Integration test for {@link KafkaNotificationConsumer}.
 *
 * Uses an in-process {@code @EmbeddedKafka} broker so no external Kafka is required.
 * The {@link EmailService} is mocked so we can assert it was called with the correct
 * arguments after the consumer processes the event.
 *
 * Pattern:
 *   1. Directly publish a synthetic event via KafkaTemplate (bypassing the publisher service).
 *   2. Wait (via Awaitility) for the async consumer to process it.
 *   3. Verify the EmailService mock was invoked with expected arguments.
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {
                KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED,
                KafkaTopicConfig.TOPIC_USER_REGISTERED,
                KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED_DLT,
                KafkaTopicConfig.TOPIC_USER_REGISTERED_DLT
        },
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:${embeddedKafka.port:9099}",
                "port=${embeddedKafka.port:9099}"
        }
)
class KafkaNotificationConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private EmailService emailService;

    // ── OTP event ───────────────────────────────────────────────────────────

    @Test
    void whenOtpEventPublished_thenEmailServiceSendOtpIsCalled() {
        OtpVerificationRequestedEvent event = OtpVerificationRequestedEvent.builder()
                .correlationId(UUID.randomUUID())
                .email("test@example.com")
                .otpCode("123456")
                .requestedAt(OffsetDateTime.now())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED, event.getEmail(), event);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() ->
                verify(emailService).sendOtpEmail(
                        eq("test@example.com"),
                        eq("123456")
                )
        );
    }

    // ── User registered event ───────────────────────────────────────────────

    @Test
    void whenUserRegisteredEventPublished_thenEmailServiceSendEmailIsCalled() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .correlationId(UUID.randomUUID())
                .userId(userId)
                .email("newuser@example.com")
                .displayName("Miku")
                .registeredAt(OffsetDateTime.now())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.TOPIC_USER_REGISTERED, event.getEmail(), event);

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendEmail(
                    eq("newuser@example.com"),
                    subjectCaptor.capture(),
                    bodyCaptor.capture()
            );
            // Assert welcome email content
            org.junit.jupiter.api.Assertions.assertTrue(
                    subjectCaptor.getValue().contains("Welcome"),
                    "Subject should contain 'Welcome'"
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                    bodyCaptor.getValue().contains("Miku"),
                    "Body should contain the user's display name"
            );
        });
    }
}
