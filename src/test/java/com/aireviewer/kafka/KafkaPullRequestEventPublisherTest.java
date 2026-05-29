package com.aireviewer.kafka;

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aireviewer.model.PullRequestEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Verifies the publisher actually produces a keyed JSON message to Kafka, using
 * an in-JVM KRaft broker (no Docker required).
 */
class KafkaPullRequestEventPublisherTest {

    private EmbeddedKafkaKraftBroker broker;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        broker = new EmbeddedKafkaKraftBroker(1, 1, KafkaTopics.REVIEW_REQUESTED);
        broker.afterPropertiesSet();

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    @AfterEach
    void tearDown() {
        broker.destroy();
    }

    @Test
    void publish_sends_a_keyed_json_event_to_the_review_requested_topic() {
        PullRequestEvent event = new PullRequestEvent(7, "octo/repo", "octo",
                "headsha", "basesha", "diffurl", "opened", "octo", "d-1",
                Instant.parse("2026-05-29T00:00:00Z"));

        new KafkaPullRequestEventPublisher(kafkaTemplate).publish(event);
        kafkaTemplate.flush();

        try (Consumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of(KafkaTopics.REVIEW_REQUESTED));
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                    consumer, KafkaTopics.REVIEW_REQUESTED, Duration.ofSeconds(10));

            assertThat(record.key()).isEqualTo("octo/repo#7");
            assertThat(record.value()).contains("\"repoFullName\":\"octo/repo\"");
            assertThat(record.value()).contains("\"prNumber\":7");
        }
    }

    private Consumer<String, String> consumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(GROUP_ID_CONFIG, "test-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
