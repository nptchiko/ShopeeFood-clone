package org.intern.shopeefoodclone.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer configuration.
 *
 * Key design decisions:
 * - acks=all: the broker waits for acknowledgment from ALL in-sync replicas before confirming
 *   a write. This prevents data loss if the broker leader crashes after acknowledging.
 * - enable.idempotence=true: combined with acks=all and retries, this guarantees that
 *   a message is written exactly once even when the producer retries after a network error.
 * - JacksonJsonSerializer: events are serialized as JSON with a type header, enabling the
 *   consumer's JacksonJsonDeserializer to reconstruct the correct event class automatically.
 */
@Slf4j
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Embed the Java type info into the Kafka record header so the consumer
        // can deserialize back to the exact event class without a type mapping registry.
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        log.info("KafkaProducerFactory configured: brokers={}, acks=all, idempotent=true", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
