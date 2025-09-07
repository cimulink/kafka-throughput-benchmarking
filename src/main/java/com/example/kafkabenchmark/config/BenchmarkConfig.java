package com.example.kafkabenchmark.config;

public class BenchmarkConfig {
    private String bootstrapServers;
    private String[] brokerJmxEndpoints;
    private String topic;
    private int partitions;
    private long messages;
    private KeyCardinality keyCardinality;
    private int throughputTarget;
    private int payloadSize;

    public enum KeyCardinality {
        LOW, HIGH
    }

    // Constructors
    public BenchmarkConfig() {}

    public BenchmarkConfig(String bootstrapServers, String brokerJmxEndpoints, String topic, 
                          int partitions, long messages, String keyCardinality, 
                          int throughputTarget, int payloadSize) {
        this.bootstrapServers = bootstrapServers;
        this.brokerJmxEndpoints = brokerJmxEndpoints.split(",");
        this.topic = topic;
        this.partitions = partitions;
        this.messages = messages;
        this.keyCardinality = KeyCardinality.valueOf(keyCardinality.toUpperCase());
        this.throughputTarget = throughputTarget;
        this.payloadSize = payloadSize;
    }

    // Getters and Setters
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String[] getBrokerJmxEndpoints() {
        return brokerJmxEndpoints;
    }

    public void setBrokerJmxEndpoints(String[] brokerJmxEndpoints) {
        this.brokerJmxEndpoints = brokerJmxEndpoints;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public long getMessages() {
        return messages;
    }

    public void setMessages(long messages) {
        this.messages = messages;
    }

    public KeyCardinality getKeyCardinality() {
        return keyCardinality;
    }

    public void setKeyCardinality(KeyCardinality keyCardinality) {
        this.keyCardinality = keyCardinality;
    }

    public int getThroughputTarget() {
        return throughputTarget;
    }

    public void setThroughputTarget(int throughputTarget) {
        this.throughputTarget = throughputTarget;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    @Override
    public String toString() {
        return "BenchmarkConfig{" +
                "bootstrapServers='" + bootstrapServers + '\'' +
                ", brokerJmxEndpoints=" + java.util.Arrays.toString(brokerJmxEndpoints) +
                ", topic='" + topic + '\'' +
                ", partitions=" + partitions +
                ", messages=" + messages +
                ", keyCardinality=" + keyCardinality +
                ", throughputTarget=" + throughputTarget +
                ", payloadSize=" + payloadSize +
                '}';
    }
}