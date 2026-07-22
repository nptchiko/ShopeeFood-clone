package org.intern.shopeefoodclone.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer configuration.
 *
 * Key design decisions:
 * - MANUAL_IMMEDIATE ack mode: offset is committed only after the listener method returns successfully.
 *   This guarantees at-least-once delivery — a message is never lost even if the consumer crashes mid-processing.
 * - ErrorHandlingDeserializer wraps JacksonJsonDeserializer: deserialization errors are caught and routed to DLT
 *   instead of poisoning the consumer loop.
 * - DefaultErrorHandler with FixedBackOff(2000ms, 3 retries): transient failures (e.g., SMTP blip)
 *   are retried before the message is forwarded to the Dead Letter Topic.
 * - Concurrency = 3: up to 3 consumer threads, matching the 3 topic partitions for maximum throughput.
 */
@Slf4j
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ── Consumer Factory ────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // ErrorHandlingDeserializer wraps JacksonJsonDeserializer so that a malformed
        // message record doesn't kill the entire consumer thread.
        ErrorHandlingDeserializer<Object> valueDeserializer = new ErrorHandlingDeserializer<>(
                new JacksonJsonDeserializer<>(Object.class, false)
                        .trustedPackages("org.intern.shopeefoodclone.events")
        );

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    // ── Listener Container Factory ──────────────────────────────────────────

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        // MANUAL_IMMEDIATE: listener must call Acknowledgment.acknowledge() to commit offset
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Scale consumers up to 3 threads (one per partition)
        factory.setConcurrency(3);

        // Error handler: retry 3 times with 2 s delay, then publish to DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2_000L, 3L)  // 2 s interval, max 3 attempts
        );
        factory.setCommonErrorHandler(errorHandler);

        log.info("KafkaListenerContainerFactory configured: concurrency=3, ack=MANUAL_IMMEDIATE, retries=3");
        return factory;
    }
}
