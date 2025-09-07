package com.example.kafkabenchmark.report;

import com.example.kafkabenchmark.consumer.BenchmarkConsumer;
import com.example.kafkabenchmark.metrics.JmxMetricsCollector;
import com.example.kafkabenchmark.producer.BenchmarkProducer;
import com.opencsv.CSVWriter;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);
    
    public static void generateReport(String topic, BenchmarkProducer producer, BenchmarkConsumer consumer, 
                                    JmxMetricsCollector jmxCollector, long startTime, long endTime) {
        logger.info("Generating benchmark report for topic: {}", topic);
        
        // Generate console report
        generateConsoleReport(producer, consumer, jmxCollector, startTime, endTime);
        
        // Generate CSV report
        generateCsvReport(topic, producer, consumer, startTime, endTime);
        
        // Generate partition distribution report
        generatePartitionDistributionReport(consumer);
        
        // Generate broker metrics report
        generateBrokerMetricsReport(jmxCollector);
    }
    
    private static void generateConsoleReport(BenchmarkProducer producer, BenchmarkConsumer consumer, 
                                            JmxMetricsCollector jmxCollector, long startTime, long endTime) {
        long totalTimeSeconds = (endTime - startTime) / 1000;
        long totalMessages = producer.getSentMessages();
        double avgThroughput = totalMessages / (double) totalTimeSeconds;
        
        Histogram producerLatencyHist = producer.getLatencyHistogram();
        Histogram consumerLatencyHist = consumer.getLatencyHistogram();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("FINAL BENCHMARK REPORT");
        System.out.println("=".repeat(80));
        System.out.printf("Total Execution Time: %d seconds%n", totalTimeSeconds);
        System.out.printf("Total Messages: %d%n", totalMessages);
        System.out.printf("Average Throughput: %.2f messages/second%n", avgThroughput);
        System.out.println();
        
        System.out.println("PRODUCER LATENCY (ms):");
        System.out.printf("  50th Percentile: %d%n", producerLatencyHist.getValueAtPercentile(50.0));
        System.out.printf("  95th Percentile: %d%n", producerLatencyHist.getValueAtPercentile(95.0));
        System.out.printf("  99th Percentile: %d%n", producerLatencyHist.getValueAtPercentile(99.0));
        System.out.printf("  Max Latency: %d%n", producerLatencyHist.getMaxValue());
        System.out.println();
        
        System.out.println("CONSUMER LATENCY (ms):");
        System.out.printf("  50th Percentile: %d%n", consumerLatencyHist.getValueAtPercentile(50.0));
        System.out.printf("  95th Percentile: %d%n", consumerLatencyHist.getValueAtPercentile(95.0));
        System.out.printf("  99th Percentile: %d%n", consumerLatencyHist.getValueAtPercentile(99.0));
        System.out.printf("  Max Latency: %d%n", consumerLatencyHist.getMaxValue());
        System.out.println();
    }
    
    private static void generateCsvReport(String topic, BenchmarkProducer producer, BenchmarkConsumer consumer, 
                                        long startTime, long endTime) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = String.format("benchmark_results_%s_%s.csv", topic, timestamp);
        
        try (CSVWriter writer = new CSVWriter(new FileWriter(filename))) {
            // Write header
            writer.writeNext(new String[]{"timestamp", "lag", "p95_latency_ms", "p99_latency_ms"});
            
            // Write data (in a real implementation, we would have time-series data)
            Histogram consumerLatencyHist = consumer.getLatencyHistogram();
            long totalLag = consumer.getPartitionLag().values().stream().mapToLong(Long::longValue).sum();
            
            writer.writeNext(new String[]{
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(totalLag),
                String.valueOf(consumerLatencyHist.getValueAtPercentile(95.0)),
                String.valueOf(consumerLatencyHist.getValueAtPercentile(99.0))
            });
            
            logger.info("CSV report generated: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to generate CSV report", e);
        }
    }
    
    private static void generatePartitionDistributionReport(BenchmarkConsumer consumer) {
        System.out.println("PARTITION DISTRIBUTION:");
        Map<Integer, Long> partitionLag = consumer.getPartitionLag();
        for (Map.Entry<Integer, Long> entry : partitionLag.entrySet()) {
            System.out.printf("  Partition %d: %d messages%n", entry.getKey(), entry.getValue());
        }
        System.out.println();
    }
    
    private static void generateBrokerMetricsReport(JmxMetricsCollector jmxCollector) {
        System.out.println("BROKER METRICS:");
        Map<String, Map<String, Double>> brokerMetrics = jmxCollector.getBrokerMetrics();
        
        for (Map.Entry<String, Map<String, Double>> brokerEntry : brokerMetrics.entrySet()) {
            String broker = brokerEntry.getKey();
            Map<String, Double> metrics = brokerEntry.getValue();
            
            System.out.printf("  Broker %s:%n", broker);
            for (Map.Entry<String, Double> metricEntry : metrics.entrySet()) {
                System.out.printf("    %s: %.2f%n", metricEntry.getKey(), metricEntry.getValue());
            }
        }
        System.out.println();
    }
}