package org.intern.shopeefoodclone.infras.messaging;

import org.intern.shopeefoodclone.config.kafka.KafkaTopicConfig;
import org.intern.shopeefoodclone.events.OtpVerificationRequestedEvent;
import org.intern.shopeefoodclone.events.UserRegisteredEvent;
import org.intern.shopeefoodclone.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link KafkaEventPublisher}.
 *
 * Mocks the {@link KafkaTemplate} to verify that the publisher:
 *   - Sends to the correct topic
 *   - Uses the email address as the partition key
 *   - Builds event payloads with the expected fields
 *
 * No Spring context or Kafka broker needed — pure Mockito unit test.
 */
@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventPublisher kafkaEventPublisher;

    // ── OTP publish ─────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void publishOtpVerificationRequested_shouldSendToCorrectTopicWithEmailAsKey() {
        // Arrange
        CompletableFuture<SendResult<String, Object>> mockFuture = new CompletableFuture<>();
        mockFuture.complete(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);

        String email = "user@example.com";
        String otpCode = "654321";

        // Act
        kafkaEventPublisher.publishOtpVerificationRequested(email, otpCode);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertEquals(KafkaTopicConfig.TOPIC_OTP_VERIFICATION_REQUESTED, topicCaptor.getValue(),
                "Should publish to the correct OTP topic");
        assertEquals(email, keyCaptor.getValue(),
                "Email should be used as the Kafka message key for partition ordering");

        OtpVerificationRequestedEvent event = (OtpVerificationRequestedEvent) payloadCaptor.getValue();
        assertEquals(email, event.getEmail());
        assertEquals(otpCode, event.getOtpCode());
        assertNotNull(event.getCorrelationId(), "Correlation ID must be set");
        assertNotNull(event.getRequestedAt(), "RequestedAt timestamp must be set");
    }

    // ── User registered publish ─────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void publishUserRegistered_shouldSendToCorrectTopicWithEmailAsKey() {
        // Arrange
        CompletableFuture<SendResult<String, Object>> mockFuture = new CompletableFuture<>();
        mockFuture.complete(mock(SendResult.class));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(mockFuture);

        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Miku Nakano")
                .email("miku@example.com")
                .passwordHash("hashed")
                .role("USER")
                .build();

        // Act
        kafkaEventPublisher.publishUserRegistered(user);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

        assertEquals(KafkaTopicConfig.TOPIC_USER_REGISTERED, topicCaptor.getValue(),
                "Should publish to the correct user.registered topic");
        assertEquals(user.getEmail(), keyCaptor.getValue(),
                "Email should be used as the Kafka message key for partition ordering");

        UserRegisteredEvent event = (UserRegisteredEvent) payloadCaptor.getValue();
        assertEquals(user.getId(), event.getUserId());
        assertEquals(user.getEmail(), event.getEmail());
        assertEquals(user.getName(), event.getDisplayName());
        assertNotNull(event.getCorrelationId(), "Correlation ID must be set");
        assertNotNull(event.getRegisteredAt(), "RegisteredAt timestamp must be set");
    }
}
