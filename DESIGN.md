# Kafka Benchmarking POC - Design Document

## Architecture Overview

The Kafka Benchmarking POC is designed as a modular, extensible Java application that demonstrates the impact of different partitioning strategies on Kafka performance. The application follows a layered architecture with clearly defined components:

```
┌─────────────────────────────────────────────────────────────┐
│                    BenchmarkApplication                     │
├─────────────────────────────────────────────────────────────┤
│                      Configuration                          │
├─────────────────────────────────────────────────────────────┤
│  TopicManager │ KeyGenerator │ Producer │ Consumer │ Metrics │
├─────────────────────────────────────────────────────────────┤
│                         Reporting                           │
└─────────────────────────────────────────────────────────────┘
```

## Component Design

### 1. BenchmarkApplication (Main Class)
- Entry point for the application
- Parses command-line arguments using Picocli
- Orchestrates the benchmark execution flow
- Manages the lifecycle of all components

### 2. Configuration
- `BenchmarkConfig`: Holds all benchmark parameters
- `KeyCardinality`: Enum for key generation strategies

### 3. Kafka Components
- `TopicManager`: Handles topic creation and deletion using Kafka AdminClient
- Manages topic lifecycle to ensure clean test environments

### 4. Key Generation
- `KeyGenerator`: Interface for key generation strategies
- `LowCardinalityKeyGenerator`: Generates keys from a fixed set of 10 values
- `HighCardinalityKeyGenerator`: Generates UUID-based keys for high cardinality
- `KeyGeneratorFactory`: Factory pattern for creating appropriate key generators

### 5. Producer
- `BenchmarkProducer`: Kafka producer implementation with throttling
- Implements rate limiting to achieve target throughput
- Tracks latency metrics using HdrHistogram
- Embeds timestamps in messages for end-to-end latency calculation

### 6. Consumer
- `BenchmarkConsumer`: Kafka consumer implementation
- Runs in a separate thread to consume messages concurrently
- Calculates end-to-end latency from embedded timestamps
- Tracks consumer lag per partition
- Commits offsets synchronously for accurate lag measurement

### 7. Metrics Collection
- `JmxMetricsCollector`: Collects broker-level metrics via JMX
- Monitors key Kafka metrics:
  - BytesInPerSec
  - MessagesInPerSec
  - RequestHandlerAvgIdlePercent
- Collects metrics periodically during benchmark execution

### 8. Reporting
- `ReportGenerator`: Generates comprehensive benchmark reports
- Produces console output with real-time progress
- Generates detailed final reports with:
  - Throughput metrics
  - Latency percentiles
  - Partition distribution
  - Broker metrics
- Creates CSV files for external analysis

## Design Patterns Used

### Factory Pattern
- `KeyGeneratorFactory` creates appropriate key generators based on configuration
- Enables easy extension for new key generation strategies

### Observer Pattern
- Consumer observes producer through Kafka topic
- JMX collector observes broker metrics

### Strategy Pattern
- Different key generation strategies implement the same interface
- Allows switching between strategies without code changes

## Performance Considerations

### Producer Throttling
- Implements precise rate limiting using nanosecond timing
- Ensures consistent message production rate for fair comparison
- Uses Kafka producer batching for efficiency

### Memory Management
- Uses HdrHistogram for efficient latency tracking
- Minimizes object creation in hot paths
- Properly closes resources to prevent memory leaks

### Concurrency
- Producer and consumer run in separate threads
- Uses thread-safe data structures for metrics collection
- Proper synchronization for shared resources

## Extensibility Features

### Modular Design
- Each component has a single responsibility
- Components can be extended or replaced independently
- Clear interfaces between components

### Configuration-Driven
- All behavior controlled through command-line parameters
- No hardcoded values that limit flexibility
- Easy to test different scenarios

### Plugin Architecture for Key Generation
- New key generation strategies can be added by implementing `KeyGenerator`
- Factory pattern makes it easy to integrate new strategies

## Error Handling

### Graceful Degradation
- Continues execution even if some metrics collection fails
- Provides meaningful error messages for troubleshooting
- Cleans up resources even in error conditions

### Resource Management
- Uses try-with-resources and finally blocks for proper cleanup
- Explicitly closes Kafka clients, JMX connections, and file handles
- Handles InterruptedException properly in multi-threaded components

## Testing Strategy

### Unit Tests
- Key generators have dedicated unit tests
- Verify correct behavior of low and high cardinality strategies
- Ensure uniqueness properties where required

### Integration Considerations
- Designed to work with real Kafka clusters
- Handles network failures gracefully
- Provides clear error messages for common configuration issues

## Security Considerations

### JMX Security
- Assumes JMX connections are properly secured in production
- No built-in authentication for JMX connections
- Should be run in trusted network environments only

### Data Handling
- No sensitive data processing
- All test data is synthetic
- No persistence of test data beyond CSV reports

## Future Enhancements

### Additional Metrics
- JVM metrics collection
- Network I/O monitoring
- Disk I/O metrics for brokers

### Advanced Scenarios
- Transactional producer/consumer testing
- Exactly-once semantics evaluation
- Different serialization formats

### Enhanced Reporting
- Real-time dashboard integration
- Historical trend analysis
- Comparative report generation