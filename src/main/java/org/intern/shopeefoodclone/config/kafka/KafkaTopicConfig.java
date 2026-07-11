package org.intern.shopeefoodclone.config.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Declares the {@link KafkaAdmin} bean and all Kafka topics.
 *
 * <p><b>Why explicit KafkaAdmin?</b><br>
 * Because we manually declared {@code ProducerFactory}, {@code ConsumerFactory},
 * and {@code KafkaTemplate} beans, Spring Boot's {@code KafkaAutoConfiguration}
 * partially backs off. Declaring {@code KafkaAdmin} explicitly guarantees that:
 * <ul>
 *   <li>The admin client carries the same reconnect-backoff and timeout settings
 *       we configured for producers/consumers (preventing a startup flood from
 *       topic-provisioning retries).</li>
 *   <li>{@link org.intern.shopeefoodclone.infras.messaging.KafkaHealthChecker}
 *       injects a fully-configured instance, not a partially auto-wired one.</li>
 *   <li>{@code fail-fast=false} keeps the app alive when Kafka is down at boot.</li>
 * </ul>
 *
 * <p><b>Partition strategy:</b><br>
 * 3 partitions — matches consumer concurrency (3 threads), enabling one-to-one
 * partition assignment for maximum parallel throughput.
 * 1 replica — suitable for local dev; increase to 3 in production.
 */
@Configuration
public class KafkaTopicConfig {

    // ── Topic name constants ────────────────────────────────────────────────

    /** Fired when a user requests an OTP verification code. */
    public static final String TOPIC_OTP_VERIFICATION_REQUESTED = "otp.verification.requested";

    /** Fired when a user completes registration (for welcome emails, analytics, etc.). */
    public static final String TOPIC_USER_REGISTERED = "user.registered";

    // ── Dead-letter topics (DLT) ────────────────────────────────────────────

    /** Receives messages that failed all retry attempts from OTP consumer. */
    public static final String TOPIC_OTP_VERIFICATION_REQUESTED_DLT = "otp.verification.requested.DLT";

    /** Receives messages that failed all retry attempts from user.registered consumer. */
    public static final String TOPIC_USER_REGISTERED_DLT = "user.registered.DLT";

    // ── KafkaAdmin bean ─────────────────────────────────────────────────────

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }


    @Bean
    public NewTopic otpVerificationRequestedTopic() {
        return TopicBuilder.name(TOPIC_OTP_VERIFICATION_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(TOPIC_USER_REGISTERED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic otpVerificationDltTopic() {
        return TopicBuilder.name(TOPIC_OTP_VERIFICATION_REQUESTED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegisteredDltTopic() {
        return TopicBuilder.name(TOPIC_USER_REGISTERED_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
