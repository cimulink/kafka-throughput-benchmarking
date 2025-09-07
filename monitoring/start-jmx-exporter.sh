#!/bin/bash
java -Dcom.sun.management.jmxremote.ssl=false \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -jar jmx_prometheus_httpserver.jar 5556 /opt/bitnami/jmx-exporter/conf/config.yml