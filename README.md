# Kafka Partitioning Strategy Benchmark POC

This project is a Java-based Proof of Concept to demonstrate the dramatic impact of a data-aware partitioning strategy versus an arbitrary one in Apache Kafka.

## Project Overview

The performance and stability of real-time data streaming infrastructure can be significantly affected by suboptimal Kafka topic partitioning. This benchmarking tool quantitatively demonstrates the impact of different partitioning strategies:

1. **Bad Scenario**: Low-cardinality keys leading to hot partitions
2. **Optimized Scenario**: High-cardinality keys for even partition distribution

## Prerequisites

- Java 17 or higher
- Apache Maven 3.6 or higher (or Gradle 8.5 or higher)
- Access to a Kafka cluster with JMX enabled
- Kafka brokers with JMX ports accessible from the machine running the benchmark

## Building the Project

### Using Maven

To build the project with Maven, run:

```bash
mvn clean package
```

This will create a fat JAR file in the `target` directory.

### Using Gradle

To build the project with Gradle, run:

```bash
gradle build
```

This will create a JAR file in the `build/libs` directory.

## Running the Benchmark

### Bad Scenario (Low Cardinality Keys)

```bash
java -jar target/kafka-benchmark-1.0-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --broker-jmx-endpoints localhost:9999 \
  --topic bad-scenario \
  --partitions 32 \
  --messages 1000000 \
  --key-cardinality LOW \
  --throughput-target 10000
```

Or with Gradle:

```bash
java -jar build/libs/kafka-benchmark-1.0-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --broker-jmx-endpoints localhost:9999 \
  --topic bad-scenario \
  --partitions 32 \
  --messages 1000000 \
  --key-cardinality LOW \
  --throughput-target 10000
```

### Optimized Scenario (High Cardinality Keys)

```bash
java -jar target/kafka-benchmark-1.0-SNAPSHOT.jar \
  --bootstrap-servers localhost:9092 \
  --broker-jmx-endpoints localhost:9999 \
  --topic optimized-scenario \
  --partitions 16 \
  --messages 1000000 \
  --key-cardinality HIGH \
  --throughput-target 10000
```

## Configuration Options

| Option | Description | Required |
|--------|-------------|----------|
| `--bootstrap-servers` | Kafka broker addresses | Yes |
| `--broker-jmx-endpoints` | Comma-separated list of broker JMX endpoints | Yes |
| `--topic` | The base name for the test topic | Yes |
| `--partitions` | The number of partitions for the topic | Yes |
| `--messages` | The total number of messages to send | Yes |
| `--key-cardinality` | LOW or HIGH key cardinality | Yes |
| `--throughput-target` | Target messages/sec for the producer | Yes |
| `--payload-size` | Size of the message payload in bytes (default: 100) | No |

## Expected Outcomes

### Bad Scenario
- Extreme partition skew with ~10 partitions containing most messages and others empty
- Consumer lag growing into millions on "hot" partitions
- High and variable end-to-end latency (seconds or minutes)
- Producer throughput meeting target (false sense of security)

### Optimized Scenario
- Evenly balanced partitions
- Low and stable consumer lag across all partitions
- Low and stable end-to-end latency (milliseconds)
- Producer throughput meeting target

## Output

The benchmark generates:
1. Real-time console output every 10 seconds
2. Final detailed console report
3. CSV file with benchmark results
4. Partition distribution report
5. Broker metrics report

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/kafkabenchmark/
│   │       ├── BenchmarkApplication.java       # Main application class
│   │       ├── config/                         # Configuration classes
│   │       ├── consumer/                       # Kafka consumer implementation
│   │       ├── key/                            # Key generation strategies
│   │       ├── kafka/                          # Kafka topic management
│   │       ├── metrics/                        # JMX metrics collection
│   │       ├── producer/                       # Kafka producer implementation
│   │       └── report/                         # Report generation
│   └── resources/
│       └── logback.xml                         # Logging configuration
├── test/
│   └── java/
│       └── com/example/kafkabenchmark/
│           └── key/                            # Key generator tests
├── pom.xml                                     # Maven configuration
├── build.gradle                                # Gradle configuration
└── README.md                                   # This file
```

## Dependencies

- Kafka Clients 3.6.0
- Picocli 4.7.5 (Command line parsing)
- HdrHistogram 2.1.12 (Latency measurements)
- OpenCSV 5.7.1 (CSV report generation)
- SLF4J 2.0.9 + Logback (Logging)

## License

This project is licensed under the MIT License.