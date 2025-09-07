package com.example.kafkabenchmark.producer;

import com.example.kafkabenchmark.config.BenchmarkConfig;
import com.example.kafkabenchmark.key.KeyGenerator;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class BenchmarkProducer {
    private static final Logger logger = LoggerFactory.getLogger(BenchmarkProducer.class);
    
    private final KafkaProducer<String, String> producer;
    private final String topic;
    private final KeyGenerator keyGenerator;
    private final int payloadSize;
    private final int throughputTarget;
    private final long totalMessages;
    
    private final AtomicLong sentMessages = new AtomicLong(0);
    private final Histogram latencyHistogram = new Histogram(3600000000L, 3);
    
    public BenchmarkProducer(BenchmarkConfig config, KeyGenerator keyGenerator) {
        this.topic = config.getTopic();
        this.keyGenerator = keyGenerator;
        this.payloadSize = config.getPayloadSize();
        this.throughputTarget = config.getThroughputTarget();
        this.totalMessages = config.getMessages();
        
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Recommended producer configurations for high throughput
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        
        this.producer = new KafkaProducer<>(props);
    }
    
    public void startProducing() throws InterruptedException {
        logger.info("Starting producer for topic: {}", topic);
        
        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        long lastSentCount = 0;
        
        // Calculate the interval between messages to achieve target throughput
        long intervalNanos = (long) (1_000_000_000.0 / throughputTarget);
        long nextSendTime = System.nanoTime();
        
        while (sentMessages.get() < totalMessages) {
            // Throttle to achieve target throughput
            long now = System.nanoTime();
            if (now < nextSendTime) {
                Thread.sleep(Math.max(1, (nextSendTime - now) / 1_000_000));
            }
            
            // Generate message with timestamp
            String key = keyGenerator.generateKey();
            String value = createMessageWithTimestamp();
            
            // Increment sent messages count before sending
            sentMessages.incrementAndGet();
            
            // Send message asynchronously for high throughput
            producer.send(new ProducerRecord<>(topic, key, value), (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send message: " + exception.getMessage());
                } else {
                    // Record latency only for successful sends
                    long latency = System.currentTimeMillis() - metadata.timestamp();
                    latencyHistogram.recordValue(latency);
                }
            });
            
            // Report progress every 10 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReportTime >= 10000) {
                long currentSentCount = sentMessages.get();
                long messagesSinceLastReport = currentSentCount - lastSentCount;
                double avgThroughput = messagesSinceLastReport / ((currentTime - lastReportTime) / 1000.0);
                
                logger.info("Sent: {}/{} messages | Producer Avg Throughput: {:.2f} msg/s", 
                        currentSentCount, totalMessages, avgThroughput);
                
                lastReportTime = currentTime;
                lastSentCount = currentSentCount;
            }
            
            nextSendTime += intervalNanos;
        }
        
        // Flush the producer to ensure all messages are sent
        producer.flush();
        
        long totalTime = System.currentTimeMillis() - startTime;
        double avgThroughput = (sentMessages.get() / (totalTime / 1000.0));
        
        logger.info("Producer finished. Total messages sent: {}, Average throughput: {:.2f} msg/s", 
                   sentMessages.get(), avgThroughput);
    }
    
    private String createMessageWithTimestamp() {
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp:").append(Instant.now().toEpochMilli());
        sb.append(",data:");
        
        // Pad with dummy data to reach payload size
        int currentSize = sb.length();
        int paddingNeeded = payloadSize - currentSize;
        
        if (paddingNeeded > 0) {
            for (int i = 0; i < paddingNeeded; i++) {
                sb.append('x');
            }
        }
        
        return sb.toString();
    }
    
    public long getSentMessages() {
        return sentMessages.get();
    }
    
    public Histogram getLatencyHistogram() {
        return latencyHistogram;
    }
    
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}