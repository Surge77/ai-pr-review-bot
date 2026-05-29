package com.aireviewer.config;

import java.util.HashMap;
import java.util.Map;

import com.aireviewer.kafka.KafkaTopics;
import com.aireviewer.model.PullRequestEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Kafka consumer wiring for the review pipeline: a JSON consumer factory and a
 * listener container factory configured for manual offset commits, retry with
 * exponential backoff (3 retries), and dead-letter routing on exhaustion.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final String CONSUMER_GROUP = "pr-review-group";
    private static final int MAX_RETRIES = 3;
    private static final long BACKOFF_INITIAL_MS = 200L;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long BACKOFF_MAX_MS = 5_000L;

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    /**
     * Consumer factory deserializing keys as strings and values as
     * {@link PullRequestEvent} JSON, wrapped in an {@link ErrorHandlingDeserializer}
     * so a poison (unparseable) record is dead-lettered rather than looping.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, PullRequestEvent> reviewConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<PullRequestEvent> jsonDeserializer =
                new JsonDeserializer<>(PullRequestEvent.class);
        jsonDeserializer.addTrustedPackages("com.aireviewer.model");

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer));
    }

    /**
     * Listener container factory with manual-immediate acknowledgment and a
     * dead-letter-publishing error handler. Records that fail processing are
     * retried {@value #MAX_RETRIES} times with exponential backoff, then routed to
     * {@link KafkaTopics#REVIEW_FAILED} on the same partition.
     *
     * @param consumerFactory the review consumer factory
     * @param kafkaTemplate   template used to publish to the dead-letter topic
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PullRequestEvent> reviewListenerContainerFactory(
            ConsumerFactory<String, PullRequestEvent> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, PullRequestEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);

        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(KafkaTopics.REVIEW_FAILED, record.partition()));

        var backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(BACKOFF_INITIAL_MS);
        backOff.setMultiplier(BACKOFF_MULTIPLIER);
        backOff.setMaxInterval(BACKOFF_MAX_MS);

        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, backOff));
        return factory;
    }
}
