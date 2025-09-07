package com.example.kafkabenchmark.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JmxMetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(JmxMetricsCollector.class);
    
    private final String[] jmxEndpoints;
    private final Map<String, JMXConnector> connectors = new HashMap<>();
    private final Map<String, MBeanServerConnection> connections = new HashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final Map<String, Map<String, Double>> brokerMetrics = new HashMap<>();
    
    public JmxMetricsCollector(String[] jmxEndpoints) {
        this.jmxEndpoints = jmxEndpoints;
        initializeConnections();
    }
    
    private void initializeConnections() {
        for (String endpoint : jmxEndpoints) {
            try {
                String jmxUrl = "service:jmx:rmi:///jndi/rmi://" + endpoint + "/jmxrmi";
                JMXServiceURL serviceURL = new JMXServiceURL(jmxUrl);
                JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
                connectors.put(endpoint, connector);
                connections.put(endpoint, connector.getMBeanServerConnection());
                brokerMetrics.put(endpoint, new HashMap<>());
                logger.info("Connected to JMX endpoint: {}", endpoint);
            } catch (Exception e) {
                logger.error("Failed to connect to JMX endpoint: {}", endpoint, e);
            }
        }
    }
    
    public void startCollecting() {
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 15, TimeUnit.SECONDS);
    }
    
    private void collectMetrics() {
        for (Map.Entry<String, MBeanServerConnection> entry : connections.entrySet()) {
            String endpoint = entry.getKey();
            MBeanServerConnection connection = entry.getValue();
            Map<String, Double> metrics = brokerMetrics.get(endpoint);
            
            try {
                // BytesInPerSec
                ObjectName bytesInName = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec");
                Object bytesInValue = connection.getAttribute(bytesInName, "OneMinuteRate");
                metrics.put("BytesInPerSec", ((Number) bytesInValue).doubleValue());
                
                // MessagesInPerSec
                ObjectName messagesInName = new ObjectName("kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec");
                Object messagesInValue = connection.getAttribute(messagesInName, "OneMinuteRate");
                metrics.put("MessagesInPerSec", ((Number) messagesInValue).doubleValue());
                
                // RequestHandlerAvgIdlePercent
                ObjectName requestHandlerName = new ObjectName("kafka.server:type=KafkaRequestHandlerPool,name=RequestHandlerAvgIdlePercent");
                Object requestHandlerValue = connection.getAttribute(requestHandlerName, "MeanRate");
                metrics.put("RequestHandlerAvgIdlePercent", ((Number) requestHandlerValue).doubleValue());
                
            } catch (Exception e) {
                logger.warn("Failed to collect metrics from broker {}: {}", endpoint, e.getMessage());
            }
        }
    }
    
    public Map<String, Map<String, Double>> getBrokerMetrics() {
        return new HashMap<>(brokerMetrics);
    }
    
    public void stop() {
        scheduler.shutdown();
        for (Map.Entry<String, JMXConnector> entry : connectors.entrySet()) {
            try {
                entry.getValue().close();
                logger.info("Closed JMX connection to {}", entry.getKey());
            } catch (Exception e) {
                logger.warn("Failed to close JMX connection to {}", entry.getKey(), e);
            }
        }
    }
}