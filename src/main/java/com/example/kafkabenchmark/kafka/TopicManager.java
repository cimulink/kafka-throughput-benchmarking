package com.example.kafkabenchmark.kafka;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class TopicManager {
    private static final Logger logger = LoggerFactory.getLogger(TopicManager.class);
    
    private final AdminClient adminClient;
    
    public TopicManager(String bootstrapServers) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        this.adminClient = AdminClient.create(props);
    }
    
    public void createTopic(String topicName, int partitions, short replicationFactor) throws ExecutionException, InterruptedException {
        logger.info("Creating topic {} with {} partitions and replication factor {}", 
                   topicName, partitions, replicationFactor);
        
        CreateTopicsResult result = adminClient.createTopics(
            Collections.singleton(
                new NewTopic(topicName, partitions, replicationFactor)
            )
        );
        
        KafkaFuture<Void> future = result.values().get(topicName);
        future.get(); // Wait for topic creation to complete
        
        logger.info("Topic {} created successfully", topicName);
    }
    
    public void deleteTopic(String topicName) throws ExecutionException, InterruptedException {
        logger.info("Deleting topic {}", topicName);
        
        DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
        KafkaFuture<Void> future = result.values().get(topicName);
        future.get(); // Wait for topic deletion to complete
        
        logger.info("Topic {} deleted successfully", topicName);
    }
    
    public boolean topicExists(String topicName) throws ExecutionException, InterruptedException {
        ListTopicsResult result = adminClient.listTopics();
        return result.names().get().contains(topicName);
    }
    
    public void close() {
        if (adminClient != null) {
            adminClient.close();
        }
    }
}