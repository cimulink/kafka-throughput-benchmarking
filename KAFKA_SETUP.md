# Kafka Setup Guide for Benchmarking

This guide explains how to set up a Kafka environment suitable for running the benchmarking POC.

## Quick Start with Docker

The easiest way to set up Kafka for testing is using Docker Compose.

### 1. Create docker-compose.yml

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.3.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.3.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9999:9999"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_JMX_PORT: 9999
      KAFKA_JMX_HOSTNAME: localhost
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
```

### 2. Start the services

```bash
docker-compose up -d
```

### 3. Verify the setup

```bash
# Check if Kafka is running
docker-compose logs kafka

# Create a test topic
docker exec -it kafka kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

## Manual Setup (Linux/macOS)

### 1. Install Java

Ensure you have Java 11 or higher installed:

```bash
java -version
```

### 2. Download Kafka

```bash
wget https://archive.apache.org/dist/kafka/3.6.0/kafka_2.13-3.6.0.tgz
tar -xzf kafka_2.13-3.6.0.tgz
cd kafka_2.13-3.6.0
```

### 3. Start Zookeeper

```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties
```

### 4. Start Kafka Broker with JMX Enabled

Create a custom server properties file `config/server-jmx.properties`:

```properties
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# see kafka.server.KafkaConfig for additional details and defaults

############################# Server Basics #############################

# The role of this server. Alternatives are 'broker' (default) or 'controller'
process.roles=broker

# The node id associated with this instance.
node.id=1

# The connect string for the controller quorum
controller.quorum.voters=1@localhost:9093

############################# Socket Server Settings #############################

# The address the socket server listens on. It will get the value returned from 
# java.net.InetAddress.getCanonicalHostName() if not configured.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#     listeners = PLAINTEXT://your.host.name:9092
listeners=PLAINTEXT://:9092

# Hostname and port the broker will advertise to producers and consumers. If not set, 
# it uses the value for "listeners" if configured.  Otherwise, it will use the value
# returned from java.net.InetAddress.getCanonicalHostName().
advertised.listeners=PLAINTEXT://localhost:9092

# A comma-separated list of the names of the listeners used by the controller.
# If no explicit mapping set in `listener.security.protocol.map`, default will be using PLAINTEXT protocol.
# This is required if running in KRaft mode.
controller.listener.names=CONTROLLER

# Maps listener names to security protocols, the default is for them to be the same. See the config documentation for more details.
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

# The number of threads that the server uses for receiving requests from the network and sending responses to the network
num.network.threads=3

# The number of threads that the server uses for processing requests, which may include disk I/O
num.io.threads=8

# The send buffer (SO_SNDBUF) used by the socket server
socket.send.buffer.bytes=102400

# The receive buffer (SO_RCVBUF) used by the socket server
socket.receive.buffer.bytes=102400

# The maximum size of a request that the socket server will accept (protection against OOM)
socket.request.max.bytes=104857600

############################# Log Basics #############################

# A comma separated list of directories under which to store log files
log.dirs=/tmp/kafka-logs

# The default number of log partitions per topic. More partitions allow greater
# parallelism for consumption, but this will also result in more files across
# the brokers.
num.partitions=1

# The number of threads per data directory to be used for log recovery at startup and flushing at shutdown.
# This value is recommended to be increased for installations with data dirs located in RAID array.
num.recovery.threads.per.data.dir=1

############################# Internal Topic Settings  #############################
# The replication factor for the group metadata internal topics "__consumer_offsets" and "__transaction_state"
# For anything other than development testing, a value greater than 1 is recommended to ensure availability such as 3.
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

############################# Log Flush Policy #############################

# Messages are immediately written to the filesystem but by default we only fsync() to sync
# the OS cache lazily. The following configurations control the flush of data to disk.
# There are a few important trade-offs here:
#    1. Durability: Unflushed data may be lost if you are not using replication.
#    2. Latency: Very large flush intervals may lead to latency spikes when the flush does occur as there will be a lot of data to flush.
#    3. Throughput: The flush is generally the most expensive operation, and a small flush interval may lead to excessive seeks.
# The settings below allow one to configure the flush policy to flush data after a period of time or
# every N messages (or both). This can be done globally and overridden on a per-topic basis.

# The number of messages to accept before forcing a flush of data to disk
#log.flush.interval.messages=10000

# The maximum amount of time a message can sit in a log before we force a flush
#log.flush.interval.ms=1000

############################# Log Retention Policy #############################

# The following configurations control the disposal of log segments. The policy can
# be set to delete segments after a period of time, or after a given size has accumulated.
# A segment will be deleted whenever *either* of these criteria are met. Deletion always happens
# from the end of the log.

# The minimum age of a log file to be eligible for deletion due to age
log.retention.hours=168

# A size-based retention policy for logs. Segments are pruned from the log unless the remaining
# segments drop below log.retention.bytes. Functions independently of log.retention.hours.
#log.retention.bytes=1073741824

# The maximum size of a log segment file. When this size is reached a new log segment will be created.
log.segment.bytes=1073741824

# The interval at which log segments are checked to see if they can be deleted according
# to the retention policies
log.retention.check.interval.ms=300000

############################# JMX Settings #############################

# Enable JMX
KAFKA_JMX_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=localhost
```

Start Kafka with JMX enabled:

```bash
JMX_PORT=9999 bin/kafka-server-start.sh config/server-jmx.properties
```

## Windows Setup

### 1. Install Java

Download and install Java from [Adoptium](https://adoptium.net/).

### 2. Download Kafka

Download the binary release from [Apache Kafka](https://kafka.apache.org/downloads).

### 3. Extract and Configure

Extract the downloaded archive and navigate to the Kafka directory.

### 4. Start Zookeeper

In one Command Prompt or PowerShell window:

```cmd
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

### 5. Start Kafka with JMX

In another Command Prompt or PowerShell window, set the JMX port and start Kafka:

```cmd
set JMX_PORT=9999
bin\windows\kafka-server-start.bat config\server.properties
```

To enable JMX properly on Windows, you might need to modify the `bin\windows\kafka-server-start.bat` file to include JMX options:

```bat
set KAFKA_JMX_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=localhost
```

## Verifying Your Setup

### 1. Check if services are running

```bash
# Check if Kafka is listening on port 9092
netstat -an | grep 9092

# Check if JMX is listening on port 9999
netstat -an | grep 9999
```

### 2. Test Kafka functionality

```bash
# Create a topic
kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Produce a message
echo "Hello Kafka" | kafka-console-producer.sh --topic test-topic --bootstrap-server localhost:9092

# Consume the message
kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server localhost:9092
```

### 3. Test JMX connectivity

You can use jconsole (included with JDK) to verify JMX connectivity:

```bash
jconsole localhost:9999
```

## Common Issues and Solutions

### 1. "Address already in use" errors

Make sure no other Kafka or Zookeeper instances are running:

```bash
# Kill processes using port 2181 (Zookeeper)
lsof -i :2181
kill -9 <PID>

# Kill processes using port 9092 (Kafka)
lsof -i :9092
kill -9 <PID>
```

### 2. JMX connection refused

Ensure JMX is properly configured and the firewall allows connections on port 9999.

### 3. "Could not find or load main class" on Windows

Use the `.bat` scripts instead of `.sh` scripts on Windows:
- Use `bin\windows\kafka-server-start.bat` instead of `bin/kafka-server-start.sh`

## Performance Tuning for Benchmarking

For accurate benchmarking results, consider these tuning options:

### 1. Increase heap size

Set environment variables before starting Kafka:

```bash
export KAFKA_HEAP_OPTS="-Xmx2g -Xms2g"
```

### 2. Optimize Kafka producer settings

When running the benchmark, use these producer configurations:
- `batch.size=16384`
- `linger.ms=5`
- `compression.type=snappy`

### 3. Use SSD storage

Ensure Kafka logs are stored on fast SSD storage for consistent performance.

## Security Considerations

For benchmarking purposes, the setup disables authentication and encryption. In production environments:

1. Enable SASL/SSL authentication
2. Configure proper firewall rules
3. Use strong passwords for JMX access
4. Restrict JMX access to trusted networks only