package com.example.kafkabenchmark.consumer;

import com.example.kafkabenchmark.config.BenchmarkConfig;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.HdrHistogram.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BenchmarkConsumer.class);
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("timestamp:(\\d+)");
    
    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private final int numPartitions;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong totalConsumed = new AtomicLong(0);
    private final Histogram latencyHistogram = new Histogram(3600000000L, 3);
    private final Map<Integer, Long> partitionLag = new HashMap<>();
    
    public BenchmarkConsumer(BenchmarkConfig config) {
        this.topic = config.getTopic();
        this.numPartitions = config.getPartitions();
        
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "benchmark-consumer-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        
        this.consumer = new KafkaConsumer<>(props);
    }
    
    public void startConsuming(long expectedMessages) throws InterruptedException {
        logger.info("Starting consumer for topic: {} with expectedMessages {}", topic, expectedMessages);
        
        // Subscribe to the topic
        consumer.subscribe(Collections.singletonList(topic));
        
        long lastReportTime = System.currentTimeMillis();
        long lastConsumedCount = 0;
        
        while (running.get() && totalConsumed.get() < expectedMessages) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            
            long consumedInBatch = 0;
            for (ConsumerRecord<String, String> record : records) {
                // Extract timestamp from message
                long produceTime = extractTimestamp(record.value());
                long consumeTime = System.currentTimeMillis();
                long latency = consumeTime - produceTime;
                
                latencyHistogram.recordValue(latency);
                totalConsumed.incrementAndGet();
                consumedInBatch++;
                
                // Early exit if we've consumed all expected messages
                if (totalConsumed.get() >= expectedMessages) {
                    break;
                }
            }
            
            if (consumedInBatch > 0) {
                consumer.commitSync();
            }
            
            // Update partition lag
            updatePartitionLag();
            
            // Report progress every 10 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReportTime >= 10000) {
                long currentConsumedCount = totalConsumed.get();
                long messagesSinceLastReport = currentConsumedCount - lastConsumedCount;
                double avgThroughput = messagesSinceLastReport / ((currentTime - lastReportTime) / 1000.0);
                long totalLag = calculateTotalLag();
                
                logger.info("Consumed: {}/{} messages | Consumer Avg Throughput: {:.2f} msg/s | Consumer Lag: {} | p99 Latency: {:.2f} ms",
                        currentConsumedCount, expectedMessages, avgThroughput, totalLag,
                        latencyHistogram.getValueAtPercentile(99.0));
                
                lastReportTime = currentTime;
                lastConsumedCount = currentConsumedCount;
            }
        }
        
        logger.info("Consumer finished. Total messages consumed: {}", totalConsumed.get());
    }
    
    private long extractTimestamp(String message) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse timestamp from message: {}", message);
            }
        }
        return System.currentTimeMillis();
    }
    
    private void updatePartitionLag() {
        try {
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
            Map<TopicPartition, Long> currentOffsets = new HashMap<>();
            
            for (TopicPartition partition : consumer.assignment()) {
                OffsetAndMetadata offsetAndMetadata = consumer.committed(partition);
                long committedOffset = (offsetAndMetadata != null) ? offsetAndMetadata.offset() : 0;
                currentOffsets.put(partition, committedOffset);
            }
            
            partitionLag.clear();
            for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
                TopicPartition partition = entry.getKey();
                long endOffset = entry.getValue();
                long currentOffset = currentOffsets.getOrDefault(partition, 0L);
                long lag = Math.max(0, endOffset - currentOffset);
                partitionLag.put(partition.partition(), lag);
            }
        } catch (Exception e) {
            logger.warn("Could not update partition lag", e);
        }
    }
    
    private long calculateTotalLag() {
        return partitionLag.values().stream().mapToLong(Long::longValue).sum();
    }
    
    public Map<Integer, Long> getPartitionLag() {
        return new HashMap<>(partitionLag);
    }
    
    public long getTotalConsumed() {
        return totalConsumed.get();
    }
    
    public Histogram getLatencyHistogram() {
        return latencyHistogram;
    }
    
    public void stop() {
        running.set(false);
    }
    
    public void close() {
        if (consumer != null) {
            consumer.close();
        }
    }
}