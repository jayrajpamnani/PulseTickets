# Platform Monitoring Guide

This guide details how to run Prometheus, monitor metrics across PulseTickets microservices, and handle alerts.

## 1. Architecture & Metric Collection

Each Spring Boot service exposes Prometheus metrics via Spring Boot Actuator at `/actuator/prometheus`.

| Service | Port | Metric Endpoint | Scrape Target |
| :--- | :--- | :--- | :--- |
| `api-gateway` | 8080 | `/actuator/prometheus` | `api-gateway:8080` |
| `user-service` | 8081 | `/actuator/prometheus` | `user-service:8081` |
| `event-service` | 8082 | `/actuator/prometheus` | `event-service:8082` |
| `ticket-service` | 8083 | `/actuator/prometheus` | `ticket-service:8083` |
| `payment-service` | 8084 | `/actuator/prometheus` | `payment-service:8084` |
| `notification-service` | 8085 | `/actuator/prometheus` | `notification-service:8085` |
| `analytics-service` | 8086 | `/actuator/prometheus` | `analytics-service:8086` |

---

## 2. Running Prometheus Locally & in Kubernetes

### Local Setup (Docker / Standalone)
Run Prometheus with the project's configuration:
```bash
docker run -d --name prometheus \
  -p 9090:9090 \
  -v $(pwd)/monitoring/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v $(pwd)/monitoring/alerts.yml:/etc/prometheus/alerts.yml \
  prom/prometheus
```

Access the Prometheus Web UI at `http://localhost:9090`.

---

## 3. Key Metrics to Inspect

### HTTP Server Latency & Requests
- **Request Volume**: `sum(rate(http_server_requests_seconds_count[5m])) by (service, uri)`
- **P95 Latency**: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))`
- **Error Rate (5xx)**: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100`

### Kafka Consumer Metrics
- **Consumer Lag**: `kafka_consumergroup_lag{topic=~"reservation-created|payment-completed"}`
- **Records Processed Rate**: `rate(kafka_consumer_records_consumed_total[5m])`

### JVM & System Metrics
- **Heap Memory Usage**: `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100`
- **System CPU Utilization**: `system_cpu_usage`

---

## 4. Responding to Active Alerts

### High HTTP 5xx Error Rate (`HighErrorRate`)
- **Trigger**: 5xx errors > 5% for over 2 minutes.
- **Action**: Check service logs (`kubectl logs deployment/<service> -n pulse-tickets`), check database connectivity, and review recent deployment commits.

### High Latency (`HighLatency`)
- **Trigger**: P95 latency > 2s for over 5 minutes.
- **Action**: Inspect DB connection pool (`hikaricp_active_connections`), review CPU usage, and check for unindexed queries using `EXPLAIN ANALYZE`.

### Service Instance Down (`ServiceDown`)
- **Trigger**: Scrape target `up == 0` for over 1 minute.
- **Action**: Check Kubernetes pod status (`kubectl get pods -n pulse-tickets`) and check pod events (`kubectl describe pod <pod-name>`).
