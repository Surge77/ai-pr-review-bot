package com.aireviewer.config;

import com.aireviewer.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the pipeline's Kafka topics. {@code KafkaAdmin} creates them on
 * startup. Replication factor is 1 for local single-broker use.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    /** Inbound review requests. */
    @Bean
    public NewTopic reviewRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.REVIEW_REQUESTED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    /** Dead-letter topic for events that exhaust retries. */
    @Bean
    public NewTopic reviewFailedTopic() {
        return TopicBuilder.name(KafkaTopics.REVIEW_FAILED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
