# **Product Requirements Document: Kafka Partitioning Strategy Benchmark POC**

| Status: Draft | Version: 1.0 | Date: 2025-09-07 |
| :---- | :---- | :---- |

## **1\. Executive Summary**

The performance and stability of our real-time data streaming infrastructure are frequently hampered by suboptimal Kafka topic partitioning. Teams often choose partition counts arbitrarily, leading to data skew, "hot partitions," and unpredictable processing latency. This results in SLA breaches, operational firefighting, and inefficient resource utilization.

This document outlines the requirements for a Java-based Proof of Concept (POC) to **quantitatively demonstrate the dramatic impact of a data-aware partitioning strategy versus an arbitrary one.** We will build a benchmarking tool that simulates two scenarios: a poorly configured topic with low-cardinality keys, and an optimized topic based on key cardinality.

The primary goal is to produce definitive, visual evidence that an optimized strategy leads to lower, predictable latency and stable consumer performance, backed by broker-side metrics showing a balanced load. The output of this POC will be a set of clear graphs and reports that will serve as the basis for new internal best practices for Kafka topic creation.

## **2\. Problem Statement**

When development teams create Kafka topics, the choice of partition count is often a guess ("let's start with 32"). This decision is disconnected from the nature of the data, specifically the **cardinality of the message keys** used for routing.

This leads to a critical problem known as **"hot partitions"**:

* When a topic with many partitions (e.g., 32\) receives messages with few unique keys (e.g., 5-10 "event types"), the default hash-based partitioner routes all messages to the same 5-10 partitions.  
* The remaining partitions sit empty and unused, wasting resources.  
* The few "hot" partitions become a bottleneck for the entire system. Brokers handling these partitions experience high I/O and CPU load, and their request handler threads become saturated.  
* Most importantly, the consumer instances assigned to these hot partitions are overwhelmed and develop **catastrophic consumer lag**, often growing into the millions of messages. This means real-time data is processed minutes or hours late.  
* The resulting end-to-end latency is extremely high and wildly unpredictable, causing cascading failures in downstream systems and violating data processing SLAs.

Our current monitoring often only shows aggregate producer throughput, which can be misleadingly high in these scenarios, masking the severe underlying latency, consumer lag, and broker-level stress. We lack a concrete, demonstrable tool to educate teams on the right way to configure topics.

## **3\. Goals and Objectives**

* **Prove the Hypothesis:** Quantitatively prove that matching partition strategy to key cardinality results in superior system performance, defined by low latency and minimal consumer lag.  
* **Generate Educational Assets:** Produce clear, compelling data visualizations (graphs, tables) that can be used to establish new engineering best practices for Kafka.  
* **Debunk Naive Metrics:** Demonstrate why producer-only throughput is a misleading indicator of system health and why end-to-end latency and consumer lag are the true measures of performance.

### **Secondary Goals**

* **Create a Reusable Tool:** The Java application built for this POC should be configurable and extensible enough to be used for future Kafka performance testing.  
* **Establish a Baseline:** Provide a standardized benchmark for evaluating future changes to our Kafka infrastructure or client configurations.

## **4\. Scope**

### **In Scope**

* A command-line driven Java application capable of running a full benchmark scenario.  
* Functionality to create Kafka topics with specified partition counts and configurations.  
* A configurable Kafka producer that can generate messages with both low-cardinality (e.g., repeated strings) and high-cardinality (e.g., UUID) keys.  
* Producer-side throttling to ensure a consistent message rate for fair comparison.  
* A configurable Kafka consumer group that runs concurrently with the producer to measure latency and lag.  
* Measurement and logging of the following key metrics:  
  * End-to-end latency (p95, p99) from produce time to consume time.  
  * Producer send rate (messages/sec).  
  * Consumer group lag (per partition and aggregate).  
  * Final partition data distribution (message count per partition).  
  * Key Kafka broker metrics (e.g., BytesInPerSec, RequestHandlerAvgIdlePercent) collected via JMX to show load distribution.  
* Output of benchmark results to the console in a clear, readable format and to a CSV file for easy graphing.

### **Out of Scope**

* A graphical user interface (GUI). This is a CLI-only tool.  
  * Automated deployment of a Kafka cluster. The POC assumes a pre-existing cluster to run against.  
  * Benchmarking of different serialization formats (e.g., Avro vs. Protobuf). The focus is purely on partitioning.  
  * Advanced Kafka features like transactions, exactly-once semantics, or Kafka Streams processing.  
  * Integration with a persistent metrics backend like Prometheus. Outputting to CSV/console is sufficient for the POC.

## **5\. Personas / Target Audience**

* **Platform Engineers:** Will use the tool to validate cluster configurations and establish default policies.  
* **Data Engineers & Architects:** Will use the results to design more robust and reliable data pipelines.  
* **Software Developers:** Will use the learnings from the POC to correctly configure topics for their microservices.

## **6\. Functional Requirements**

The system shall be a Java application (kafka-benchmark.jar) runnable from the command line.

### **FR-1: Scenario Configuration**

The application must be configurable via command-line arguments.

* \--bootstrap-servers: Kafka broker addresses.  
* \--broker-jmx-endpoints: Comma-separated list of broker JMX endpoints (e.g., broker1:9999,broker2:9999).  
* \--topic: The base name for the test topic. The app will append suffixes (e.g., my-test-bad, my-test-optimized).  
* \--partitions: The number of partitions for the topic.  
* \--messages: The total number of messages to send.  
  * \--key-cardinality: LOW or HIGH. LOW will use a fixed set of 10 keys. HIGH will use UUID.randomUUID().  
  * \--throughput-target: The target messages/sec for the producer.  
  * \--payload-size: The size of the message payload in bytes.

### **FR-2: Test Orchestration**

The application will have a main orchestrator that performs the following steps:

1. Parses CLI arguments.  
2. Creates the target Kafka topic with the specified number of partitions.  
3. Initializes and starts the consumer group threads.  
4. Initializes and starts the producer, which will run until the target message count is reached.  
5. Waits for consumers to process all messages (or until a timeout).  
6. Gathers all metrics from producers and consumers.  
7. Prints the final summary report to the console and saves it to a CSV file.  
8. Deletes the test topic upon completion.

### **FR-3: Kafka Producer**

* Must embed the production timestamp (in milliseconds) in the message payload or headers to calculate end-to-end latency.  
* Must implement the key generation strategy based on the \--key-cardinality flag.  
* Must implement rate-limiting/throttling to adhere to the \--throughput-target.  
* Must report the final average throughput achieved.

### **FR-4: Kafka Consumer Group**

* Must run in a separate thread pool, concurrently with the producer.  
* The number of consumer instances should be equal to the number of partitions.  
* Upon receiving a message, it must:  
  1. Extract the production timestamp.  
  2. Calculate the end-to-end latency (System.currentTimeMillis() \- productionTimestamp).  
  3. Record the latency measurement locally.  
* The consumer group must periodically report its partition-level and aggregate lag using the Kafka Admin Client APIs. This should be logged to the console during the run.

### **FR-5: Metrics & Reporting**

* **Real-time Output:** During the benchmark, the application should print a status line to the console every 10 seconds, showing:  
  * Sent: X/Y messages | Producer Avg Throughput: Z msg/s | Consumer Lag: L | p99 Latency: P ms  
* **Final Console Report:** A detailed summary printed at the end of the run, including a section for broker-level metrics.  
* **CSV Output:** A file named benchmark\_results\_{topic}\_{timestamp}.csv containing raw latency and lag measurements over time, suitable for plotting. It should contain columns: timestamp, lag, p95\_latency\_ms, p99\_latency\_ms.  
* **Partition Distribution Report:** A final table showing the total message count for each partition to visually demonstrate data skew.  
* **Broker Metrics Report:** A final table comparing key JMX metrics across all monitored brokers, showing the difference in load between the "bad" and "optimized" scenarios.

### **FR-6: Broker Metrics Collection (New)**

* The application must be able to connect to the JMX endpoints of the Kafka brokers specified via the \--broker-jmx-endpoints argument.  
* During the benchmark run, it should periodically (e.g., every 15 seconds) poll and record the following JMX MBean attributes for each broker:  
  * kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec  
  * kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec  
  * kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce (mean)  
  * kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent (mean)  
* The final report must aggregate these metrics (e.g., average, max) per broker to clearly illustrate load imbalance.

## **7\. Non-Functional Requirements**

* **NFR-1: Performance:** The tool must be capable of generating load up to 500,000 messages/second and testing with datasets of at least 100 million messages.  
* **NFR-2: Usability:** The tool must be runnable via a single, simple command line. It should require no complex setup beyond having access to a Kafka cluster. All configuration is via CLI flags.  
  * **NFR-3: Code Quality:** The Java code should be well-structured, documented, and use modern practices. It should be built using Maven or Gradle and include necessary dependencies.  
  * **NFR-4: Extensibility:** The code should be designed in a way that allows for future additions, such as new keying strategies or different metric reporting mechanisms.

## **8\. System Architecture & Technical Stack**

The POC will consist of a single runnable Java application interacting with a Kafka cluster.

* **Language:** Java 17+  
* **Build Tool:** Apache Maven or Gradle  
* **Key Libraries:**  
  * kafka-clients: For producer and consumer implementation.  
* slf4j-api & logback-classic: For logging.  
* Picocli: For parsing command-line arguments.  
* HdrHistogram: For efficient and accurate latency percentile calculations.  
* opencsv (or similar): For writing CSV reports.  
* **(New)** Standard Java JMX libraries (javax.management.\*) for broker metric collection.

## **9\. Test Scenarios (Use Cases)**

The core of the POC will be executing these two scenarios back-to-back and comparing the results.

### **UC-1: The "Bad" Scenario (Arbitrary Partitioning)**

This simulates a poorly configured system.

* **CLI Command:**

java \-jar kafka-benchmark.jar \\  
  \--bootstrap-servers kafka:9092 \\  
  \--broker-jmx-endpoints kafka:9999 \\  
  \--topic bad-scenario \\  
  \--partitions 32 \\  
  \--messages 100000000 \\  
    \--key-cardinality LOW \\  
    \--throughput-target 250000  
  \`\`\`  
\* \*\*Expected Outcome:\*\*  
    \* \*\*Partition Distribution:\*\* Extreme skew, with \\\~10 partitions containing 10M messages each and 22 partitions empty.  
    \* \*\*Consumer Lag:\*\* Will grow into the millions on the "hot" partitions and remain at 0 on others.  
    \* \*\*Latency:\*\* p99 End-to-End latency will be very high (measured in seconds or minutes) and highly variable.  
    \* \*\*Producer Throughput:\*\* Will likely meet the target, giving a false sense of security.

\#\#\# UC-2: The "Optimized" Scenario (Data-Aware Partitioning)

This simulates a correctly configured system.

\* \*\*CLI Command:\*\*

java \-jar kafka-benchmark.jar

\--bootstrap-servers kafka:9092

\--broker-jmx-endpoints kafka:9999

\--topic optimized-scenario

\--partitions 16

\--messages 100000000

\--key-cardinality HIGH

\--throughput-target 250000  
\`\`\`

* **Expected Outcome:**  
  * **Partition Distribution:** Evenly balanced. All 16 partitions will contain \~6.25M messages.  
  * **Consumer Lag:** Will remain consistently low and stable across all partitions.  
  * **Latency:** p99 End-to-End latency will be low (measured in milliseconds) and stable.  
  * **Producer Throughput:** Will meet the target. The client may experience slightly higher CPU usage, which is an expected trade-off.

## **10\. Success Criteria**

5. The consumer lag for UC-1 is shown to grow uncontrollably, while for UC-2 it remains low and stable.  
6. **(New)** The broker metrics report for UC-1 shows a severe imbalance in BytesInPerSec and RequestHandlerAvgIdlePercent across brokers, while the report for UC-2 shows an even load distribution.

## **11\. Assumptions and Dependencies**

* A functional Kafka (KRaft or ZK-based) and Kraft cluster is available for testing.  
* The network connectivity between the machine running the POC and the Kafka cluster is stable.  
* The machine running the benchmark has sufficient resources (CPU, RAM) to not be the primary bottleneck itself.  
* The user running the tool has administrative permissions on the Kafka cluster to create and delete topics.  
* **(New)** Kafka brokers have their JMX ports enabled and are network-accessible from the machine running the benchmark tool.

## **12\. Future Work**

* Integrate with Prometheus Pushgateway to push metrics to a real dashboard for better visualization.  
* Add more complex payload generation to simulate real-world JSON/Avro objects.  
  * Add support for different client configuration parameters (e.g., batch.size, linger.ms) as command-line flags to test their impact.  
  * Containerize the application using Docker for easier distribution and execution.