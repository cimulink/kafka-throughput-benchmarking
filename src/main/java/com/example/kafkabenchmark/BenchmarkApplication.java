package com.example.kafkabenchmark;

import com.example.kafkabenchmark.config.BenchmarkConfig;
import com.example.kafkabenchmark.consumer.BenchmarkConsumer;
import com.example.kafkabenchmark.key.KeyGenerator;
import com.example.kafkabenchmark.key.KeyGeneratorFactory;
import com.example.kafkabenchmark.kafka.TopicManager;
import com.example.kafkabenchmark.metrics.JmxMetricsCollector;
import com.example.kafkabenchmark.producer.BenchmarkProducer;
import com.example.kafkabenchmark.report.ReportGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "kafka-benchmark", mixinStandardHelpOptions = true, version = "1.0",
        description = "A Kafka benchmarking tool to demonstrate the impact of partitioning strategies")
public class BenchmarkApplication implements Callable<Integer> {

    @Option(names = {"--bootstrap-servers"}, description = "Kafka broker addresses", required = true)
    private String bootstrapServers;

    @Option(names = {"--broker-jmx-endpoints"}, description = "Comma-separated list of broker JMX endpoints", required = true)
    private String brokerJmxEndpoints;

    @Option(names = {"--topic"}, description = "The base name for the test topic", required = true)
    private String topic;

    @Option(names = {"--partitions"}, description = "The number of partitions for the topic", required = true)
    private int partitions;

    @Option(names = {"--messages"}, description = "The total number of messages to send", required = true)
    private long messages;

    @Option(names = {"--key-cardinality"}, description = "LOW or HIGH. LOW will use a fixed set of 10 keys. HIGH will use UUID.randomUUID()", required = true)
    private String keyCardinality;

    @Option(names = {"--throughput-target"}, description = "The target messages/sec for the producer", required = true)
    private int throughputTarget;

    @Option(names = {"--payload-size"}, description = "The size of the message payload in bytes", defaultValue = "100")
    private int payloadSize;

    @Override
    public Integer call() throws Exception {
        System.out.println("Starting Kafka Benchmark with the following parameters:");
        System.out.println("Bootstrap Servers: " + bootstrapServers);
        System.out.println("Broker JMX Endpoints: " + brokerJmxEndpoints);
        System.out.println("Topic: " + topic);
        System.out.println("Partitions: " + partitions);
        System.out.println("Messages: " + messages);
        System.out.println("Key Cardinality: " + keyCardinality);
        System.out.println("Throughput Target: " + throughputTarget);
        System.out.println("Payload Size: " + payloadSize);

        // Create configuration
        BenchmarkConfig config = new BenchmarkConfig(
                bootstrapServers, brokerJmxEndpoints, topic, partitions, messages, 
                keyCardinality, throughputTarget, payloadSize);

        // Create components
        TopicManager topicManager = new TopicManager(bootstrapServers);
        KeyGenerator keyGenerator = KeyGeneratorFactory.createKeyGenerator(config.getKeyCardinality());
        
        // Create topic
        String fullTopicName = topic + "-" + keyCardinality.toLowerCase();
        if (topicManager.topicExists(fullTopicName)) {
            System.out.println("Topic " + fullTopicName + " already exists. Deleting it first.");
            topicManager.deleteTopic(fullTopicName);
        }
        
        topicManager.createTopic(fullTopicName, partitions, (short) 1);
        config.setTopic(fullTopicName); // Update config with full topic name
        
        // Set up JMX metrics collection
        JmxMetricsCollector jmxCollector = new JmxMetricsCollector(config.getBrokerJmxEndpoints());
        jmxCollector.startCollecting();
        
        // Set up consumer
        BenchmarkConsumer consumer = new BenchmarkConsumer(config);
        Thread consumerThread = new Thread(() -> {
            try {
                consumer.startConsuming(messages);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumerThread.start();
        
        // Allow consumer to initialize
        Thread.sleep(2000);
        
        // Set up producer and start producing
        BenchmarkProducer producer = new BenchmarkProducer(config, keyGenerator);
        long startTime = System.currentTimeMillis();
        
        try {
            producer.startProducing();
        } finally {
            
            // Wait for consumer to consume all expected messages before stopping
            long waitStartTime = System.currentTimeMillis();
            while (consumer.getTotalConsumed() < messages && 
                   (System.currentTimeMillis() - waitStartTime) < 90000) { // Wait up to 90 seconds
                try {
                    Thread.sleep(1000);
                    System.out.println("Waiting for consumer to finish... Consumed: " + consumer.getTotalConsumed() + "/" + messages);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            long endTime = System.currentTimeMillis();
            
            // Stop consumer
            consumer.stop();
            consumerThread.join(10000); // Wait up to 10 seconds for consumer to stop
            
            // Generate report
            ReportGenerator.generateReport(fullTopicName, producer, consumer, jmxCollector, startTime, endTime);
            
            // Clean up
            producer.close();
            consumer.close();
            jmxCollector.stop();
            
            // Delete topic
            try {
                topicManager.deleteTopic(fullTopicName);
                System.out.println("Topic " + fullTopicName + " deleted successfully.");
            } catch (Exception e) {
                System.err.println("Failed to delete topic: " + e.getMessage());
            }
            
            topicManager.close();
        }
        
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BenchmarkApplication()).execute(args);
        System.exit(exitCode);
    }
}