# CardDemo Migration - Monitoring and Observability Guide

## Document Information

| Item | Details |
|------|---------|
| Project | CardDemo Mainframe to Cloud Migration |
| Version | 1.0 |
| Date | January 2026 |
| Purpose | Monitoring Strategy for Migrated Application |

---

## 1. Executive Summary

This document outlines the comprehensive monitoring and observability strategy for the CardDemo application migrated from COBOL/CICS mainframe to Java Spring Boot microservices with a React frontend. The strategy covers infrastructure monitoring, application performance monitoring (APM), log management, distributed tracing, alerting, and dashboards to ensure operational excellence in the AWS cloud environment.

### 1.1 Monitoring Objectives

The monitoring strategy aims to ensure high availability with 99.9% uptime SLA, detect and respond to incidents within 5 minutes, maintain application performance with response times under 500ms for 95th percentile, track business metrics and transaction volumes, support capacity planning and cost optimization, and enable root cause analysis for rapid incident resolution.

### 1.2 Monitoring Stack Overview

| Layer | Tool | Purpose |
|-------|------|---------|
| Infrastructure | Amazon CloudWatch | AWS resource monitoring |
| APM | AWS X-Ray | Distributed tracing |
| Metrics | Prometheus + Grafana | Custom metrics and dashboards |
| Logging | Amazon CloudWatch Logs + ELK | Centralized logging |
| Alerting | Amazon SNS + PagerDuty | Incident notification |
| Synthetic | Amazon CloudWatch Synthetics | Endpoint monitoring |

---

## 2. Infrastructure Monitoring

### 2.1 AWS Resource Monitoring

#### EC2/ECS Metrics

| Metric | Description | Threshold | Alert Level |
|--------|-------------|-----------|-------------|
| CPUUtilization | CPU usage percentage | >80% for 5 min | Warning |
| CPUUtilization | CPU usage percentage | >90% for 5 min | Critical |
| MemoryUtilization | Memory usage percentage | >85% for 5 min | Warning |
| MemoryUtilization | Memory usage percentage | >95% for 5 min | Critical |
| NetworkIn/Out | Network throughput | >80% capacity | Warning |
| DiskUtilization | Disk space usage | >80% | Warning |
| DiskUtilization | Disk space usage | >90% | Critical |

#### RDS PostgreSQL Metrics

| Metric | Description | Threshold | Alert Level |
|--------|-------------|-----------|-------------|
| CPUUtilization | Database CPU | >80% for 5 min | Warning |
| FreeableMemory | Available memory | <1GB | Warning |
| FreeStorageSpace | Available storage | <20% | Warning |
| DatabaseConnections | Active connections | >80% max | Warning |
| ReadLatency | Read operation latency | >20ms | Warning |
| WriteLatency | Write operation latency | >50ms | Warning |
| ReplicaLag | Replication delay | >60 seconds | Critical |

#### CloudWatch Alarms Configuration

```yaml
# CloudWatch Alarm for RDS CPU
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  RDSCPUAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: carddemo-rds-cpu-high
      AlarmDescription: RDS CPU utilization is high
      MetricName: CPUUtilization
      Namespace: AWS/RDS
      Statistic: Average
      Period: 300
      EvaluationPeriods: 2
      Threshold: 80
      ComparisonOperator: GreaterThanThreshold
      Dimensions:
        - Name: DBInstanceIdentifier
          Value: carddemo-db
      AlarmActions:
        - !Ref AlertSNSTopic
      OKActions:
        - !Ref AlertSNSTopic

  RDSConnectionsAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: carddemo-rds-connections-high
      AlarmDescription: RDS connections approaching limit
      MetricName: DatabaseConnections
      Namespace: AWS/RDS
      Statistic: Average
      Period: 300
      EvaluationPeriods: 2
      Threshold: 400
      ComparisonOperator: GreaterThanThreshold
      Dimensions:
        - Name: DBInstanceIdentifier
          Value: carddemo-db
      AlarmActions:
        - !Ref AlertSNSTopic
```

### 2.2 Kubernetes/EKS Monitoring

#### Cluster Metrics

| Metric | Description | Threshold | Alert Level |
|--------|-------------|-----------|-------------|
| cluster_cpu_usage | Cluster CPU utilization | >75% | Warning |
| cluster_memory_usage | Cluster memory utilization | >80% | Warning |
| node_count | Number of healthy nodes | <3 | Critical |
| pod_restart_count | Pod restart frequency | >3 in 15 min | Warning |
| pending_pods | Pods in pending state | >0 for 5 min | Warning |

#### Pod-Level Metrics

```yaml
# Prometheus ServiceMonitor for Spring Boot services
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: carddemo-services
  namespace: monitoring
spec:
  selector:
    matchLabels:
      app.kubernetes.io/part-of: carddemo
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
  namespaceSelector:
    matchNames:
      - carddemo-prod
      - carddemo-staging
```

### 2.3 Network Monitoring

#### API Gateway Metrics

| Metric | Description | Threshold | Alert Level |
|--------|-------------|-----------|-------------|
| 4XXError | Client error rate | >5% | Warning |
| 5XXError | Server error rate | >1% | Critical |
| Latency | Response latency p99 | >2000ms | Warning |
| Count | Request count | Baseline +50% | Info |
| IntegrationLatency | Backend latency | >1500ms | Warning |

#### Load Balancer Metrics

| Metric | Description | Threshold | Alert Level |
|--------|-------------|-----------|-------------|
| HealthyHostCount | Healthy targets | <2 | Critical |
| UnHealthyHostCount | Unhealthy targets | >0 | Warning |
| TargetResponseTime | Backend response time | >1s | Warning |
| HTTPCode_ELB_5XX | Load balancer errors | >0 | Critical |
| ActiveConnectionCount | Active connections | >10000 | Warning |

---

## 3. Application Performance Monitoring (APM)

### 3.1 Spring Boot Actuator Configuration

```yaml
# application.yml for each microservice
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:development}
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.75, 0.95, 0.99
```

### 3.2 Custom Application Metrics

#### Business Metrics

| Metric Name | Type | Description | Labels |
|-------------|------|-------------|--------|
| carddemo_customers_total | Counter | Total customers created | status |
| carddemo_accounts_total | Counter | Total accounts created | status, type |
| carddemo_cards_issued | Counter | Cards issued | status |
| carddemo_transactions_total | Counter | Transactions processed | type, status |
| carddemo_transactions_amount | Histogram | Transaction amounts | type |
| carddemo_payments_total | Counter | Payments processed | method, status |
| carddemo_payments_amount | Histogram | Payment amounts | method |
| carddemo_login_attempts | Counter | Login attempts | status |
| carddemo_api_errors | Counter | API errors | service, endpoint, code |

#### Implementation Example

```java
// MetricsService.java
@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter transactionCounter;
    private final DistributionSummary transactionAmount;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.transactionCounter = Counter.builder("carddemo_transactions_total")
            .description("Total transactions processed")
            .tag("service", "transaction-service")
            .register(meterRegistry);
            
        this.transactionAmount = DistributionSummary.builder("carddemo_transactions_amount")
            .description("Transaction amounts")
            .baseUnit("dollars")
            .publishPercentiles(0.5, 0.75, 0.95, 0.99)
            .register(meterRegistry);
    }
    
    public void recordTransaction(String type, String status, BigDecimal amount) {
        Counter.builder("carddemo_transactions_total")
            .tag("type", type)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
            
        transactionAmount.record(amount.doubleValue());
    }
    
    public void recordPayment(String method, String status, BigDecimal amount) {
        Counter.builder("carddemo_payments_total")
            .tag("method", method)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
            
        DistributionSummary.builder("carddemo_payments_amount")
            .tag("method", method)
            .register(meterRegistry)
            .record(amount.doubleValue());
    }
    
    public void recordApiError(String service, String endpoint, int statusCode) {
        Counter.builder("carddemo_api_errors")
            .tag("service", service)
            .tag("endpoint", endpoint)
            .tag("code", String.valueOf(statusCode))
            .register(meterRegistry)
            .increment();
    }
}
```

### 3.3 Service-Level Metrics

#### Auth Service Metrics

| Metric | Description | SLO |
|--------|-------------|-----|
| auth_login_duration_seconds | Login request duration | p99 < 500ms |
| auth_token_refresh_duration_seconds | Token refresh duration | p99 < 200ms |
| auth_failed_logins_total | Failed login attempts | <5% of total |
| auth_active_sessions | Current active sessions | Baseline monitoring |

#### Transaction Service Metrics

| Metric | Description | SLO |
|--------|-------------|-----|
| transaction_create_duration_seconds | Transaction creation time | p99 < 300ms |
| transaction_validation_duration_seconds | Validation time | p99 < 100ms |
| transaction_rejected_total | Rejected transactions | <2% of total |
| transaction_queue_size | Pending transactions | <1000 |

#### Payment Service Metrics

| Metric | Description | SLO |
|--------|-------------|-----|
| payment_processing_duration_seconds | Payment processing time | p99 < 2s |
| payment_success_rate | Successful payments | >99% |
| payment_retry_total | Payment retries | <1% of total |
| payment_pending_count | Pending payments | <500 |

### 3.4 AWS X-Ray Distributed Tracing

#### X-Ray Configuration

```java
// XRayConfig.java
@Configuration
public class XRayConfig {
    
    @Bean
    public AWSXRayRecorder awsXRayRecorder() {
        AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard()
            .withPlugin(new EC2Plugin())
            .withPlugin(new ECSPlugin())
            .withSamplingStrategy(new LocalizedSamplingStrategy());
        
        return builder.build();
    }
    
    @Bean
    public Filter tracingFilter() {
        return new AWSXRayServletFilter("carddemo-${spring.application.name}");
    }
}
```

#### Trace Sampling Rules

```json
{
  "version": 2,
  "rules": [
    {
      "description": "Health check endpoints - minimal sampling",
      "host": "*",
      "http_method": "GET",
      "url_path": "/actuator/health*",
      "fixed_target": 0,
      "rate": 0.01
    },
    {
      "description": "Transaction endpoints - high sampling",
      "host": "*",
      "http_method": "*",
      "url_path": "/api/v1/transactions*",
      "fixed_target": 10,
      "rate": 0.5
    },
    {
      "description": "Payment endpoints - full sampling",
      "host": "*",
      "http_method": "*",
      "url_path": "/api/v1/payments*",
      "fixed_target": 10,
      "rate": 1.0
    },
    {
      "description": "Default rule",
      "host": "*",
      "http_method": "*",
      "url_path": "*",
      "fixed_target": 5,
      "rate": 0.1
    }
  ],
  "default": {
    "fixed_target": 1,
    "rate": 0.05
  }
}
```

---

## 4. Logging Strategy

### 4.1 Log Levels and Standards

#### Log Level Guidelines

| Level | Usage | Examples |
|-------|-------|----------|
| ERROR | System errors requiring immediate attention | Database connection failures, unhandled exceptions |
| WARN | Potential issues that don't stop processing | Retry attempts, deprecated API usage |
| INFO | Significant business events | Transaction completed, user logged in |
| DEBUG | Detailed diagnostic information | Method entry/exit, variable values |
| TRACE | Very detailed tracing | Full request/response bodies |

#### Structured Logging Format

```java
// LoggingConfig.java
@Configuration
public class LoggingConfig {
    
    @Bean
    public LoggingEventCompositeJsonEncoder jsonEncoder() {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setProviders(new JsonProviders<ILoggingEvent>() {{
            addTimestamp(new LoggingEventFormattedTimestampJsonProvider());
            addLogLevel(new LogLevelJsonProvider());
            addLoggerName(new LoggerNameJsonProvider());
            addMessage(new MessageJsonProvider());
            addMdc(new MdcJsonProvider());
            addStackTrace(new StackTraceJsonProvider());
            addContext(new ContextJsonProvider<>());
        }});
        return encoder;
    }
}
```

#### Log Format Example

```json
{
  "timestamp": "2026-01-28T10:30:45.123Z",
  "level": "INFO",
  "logger": "com.carddemo.transaction.TransactionService",
  "message": "Transaction processed successfully",
  "traceId": "1-5f84c7a7-example123",
  "spanId": "abc123def456",
  "service": "transaction-service",
  "environment": "production",
  "customerId": "CUST-12345",
  "accountId": "ACCT-67890",
  "transactionId": "TXN-11111",
  "amount": 125.50,
  "type": "PURCHASE",
  "duration_ms": 45
}
```

### 4.2 CloudWatch Logs Configuration

#### Log Groups Structure

| Log Group | Services | Retention |
|-----------|----------|-----------|
| /carddemo/prod/api-gateway | API Gateway | 30 days |
| /carddemo/prod/auth-service | Auth Service | 90 days |
| /carddemo/prod/customer-service | Customer Service | 30 days |
| /carddemo/prod/account-service | Account Service | 30 days |
| /carddemo/prod/card-service | Card Service | 30 days |
| /carddemo/prod/transaction-service | Transaction Service | 90 days |
| /carddemo/prod/payment-service | Payment Service | 90 days |
| /carddemo/prod/reporting-service | Reporting Service | 30 days |
| /carddemo/prod/batch-service | Batch Service | 30 days |
| /carddemo/prod/frontend | React Frontend | 14 days |

#### Log Insights Queries

```sql
-- Find all errors in the last hour
fields @timestamp, @message, service, traceId
| filter level = "ERROR"
| sort @timestamp desc
| limit 100

-- Transaction processing times
fields @timestamp, transactionId, duration_ms
| filter logger like /TransactionService/
| filter message like /processed/
| stats avg(duration_ms) as avg_duration, 
        max(duration_ms) as max_duration,
        percentile(duration_ms, 95) as p95_duration
  by bin(5m)

-- Failed login attempts by IP
fields @timestamp, @message, clientIp, username
| filter message like /login failed/
| stats count(*) as failed_attempts by clientIp
| sort failed_attempts desc
| limit 20

-- Payment processing errors
fields @timestamp, paymentId, errorCode, errorMessage
| filter service = "payment-service"
| filter level = "ERROR"
| sort @timestamp desc
| limit 50

-- Slow API requests (>1 second)
fields @timestamp, requestPath, duration_ms, statusCode
| filter duration_ms > 1000
| sort duration_ms desc
| limit 100
```

### 4.3 ELK Stack Integration (Optional)

#### Filebeat Configuration

```yaml
# filebeat.yml
filebeat.inputs:
  - type: container
    paths:
      - /var/log/containers/carddemo-*.log
    processors:
      - add_kubernetes_metadata:
          host: ${NODE_NAME}
          matchers:
            - logs_path:
                logs_path: "/var/log/containers/"

output.elasticsearch:
  hosts: ["${ELASTICSEARCH_HOST}:9200"]
  index: "carddemo-%{+yyyy.MM.dd}"
  
setup.kibana:
  host: "${KIBANA_HOST}:5601"
  
setup.ilm:
  enabled: true
  rollover_alias: "carddemo"
  pattern: "{now/d}-000001"
  policy_name: "carddemo-policy"
```

#### Index Lifecycle Policy

```json
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_size": "50gb",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

---

## 5. Alerting Strategy

### 5.1 Alert Severity Levels

| Severity | Response Time | Notification | Examples |
|----------|---------------|--------------|----------|
| P1 - Critical | 5 minutes | PagerDuty + Phone | Service down, data loss risk |
| P2 - High | 15 minutes | PagerDuty + Slack | Degraded performance, high error rate |
| P3 - Medium | 1 hour | Slack + Email | Warning thresholds, capacity concerns |
| P4 - Low | Next business day | Email | Informational, optimization opportunities |

### 5.2 Alert Definitions

#### Infrastructure Alerts

```yaml
# Prometheus AlertManager rules
groups:
  - name: carddemo-infrastructure
    rules:
      - alert: HighCPUUsage
        expr: avg(rate(container_cpu_usage_seconds_total{namespace="carddemo-prod"}[5m])) by (pod) > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage detected"
          description: "Pod {{ $labels.pod }} CPU usage is above 80%"
          
      - alert: HighMemoryUsage
        expr: container_memory_usage_bytes{namespace="carddemo-prod"} / container_spec_memory_limit_bytes > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
          description: "Pod {{ $labels.pod }} memory usage is above 85%"
          
      - alert: PodCrashLooping
        expr: rate(kube_pod_container_status_restarts_total{namespace="carddemo-prod"}[15m]) > 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Pod is crash looping"
          description: "Pod {{ $labels.pod }} is restarting frequently"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "Service {{ $labels.application }} connection pool at 90%"
```

#### Application Alerts

```yaml
groups:
  - name: carddemo-application
    rules:
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5..",namespace="carddemo-prod"}[5m])) / sum(rate(http_server_requests_seconds_count{namespace="carddemo-prod"}[5m])) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is above 1%"
          
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{namespace="carddemo-prod"}[5m])) by (le, service)) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response times"
          description: "95th percentile response time is above 2 seconds for {{ $labels.service }}"
          
      - alert: AuthServiceDown
        expr: up{job="auth-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Auth service is down"
          description: "Authentication service is not responding"
          
      - alert: PaymentProcessingFailures
        expr: rate(carddemo_payments_total{status="FAILED"}[5m]) / rate(carddemo_payments_total[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High payment failure rate"
          description: "Payment failure rate is above 5%"
          
      - alert: TransactionQueueBacklog
        expr: carddemo_transaction_queue_size > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Transaction queue backlog"
          description: "Transaction queue has more than 1000 pending items"
```

#### Business Alerts

```yaml
groups:
  - name: carddemo-business
    rules:
      - alert: UnusualTransactionVolume
        expr: abs(sum(rate(carddemo_transactions_total[1h])) - sum(rate(carddemo_transactions_total[1h] offset 1d))) / sum(rate(carddemo_transactions_total[1h] offset 1d)) > 0.5
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Unusual transaction volume"
          description: "Transaction volume differs by more than 50% from yesterday"
          
      - alert: HighValueTransactionSpike
        expr: sum(rate(carddemo_transactions_amount_sum{type="PURCHASE"}[15m])) > 100000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High value transaction spike"
          description: "Large transaction volume detected in 15-minute window"
          
      - alert: NoTransactionsProcessed
        expr: sum(rate(carddemo_transactions_total[15m])) == 0
        for: 15m
        labels:
          severity: critical
        annotations:
          summary: "No transactions being processed"
          description: "No transactions have been processed in the last 15 minutes"
```

### 5.3 Alert Routing

```yaml
# AlertManager configuration
global:
  resolve_timeout: 5m
  slack_api_url: '${SLACK_WEBHOOK_URL}'
  pagerduty_url: 'https://events.pagerduty.com/v2/enqueue'

route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default-receiver'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
      continue: true
    - match:
        severity: critical
      receiver: 'slack-critical'
    - match:
        severity: warning
      receiver: 'slack-warning'
    - match:
        severity: info
      receiver: 'slack-info'

receivers:
  - name: 'default-receiver'
    slack_configs:
      - channel: '#carddemo-alerts'
        
  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: '${PAGERDUTY_SERVICE_KEY}'
        severity: critical
        
  - name: 'slack-critical'
    slack_configs:
      - channel: '#carddemo-critical'
        color: 'danger'
        title: 'CRITICAL Alert'
        
  - name: 'slack-warning'
    slack_configs:
      - channel: '#carddemo-alerts'
        color: 'warning'
        title: 'Warning Alert'
        
  - name: 'slack-info'
    slack_configs:
      - channel: '#carddemo-info'
        color: 'good'
        title: 'Info'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'service']
```

---

## 6. Dashboards

### 6.1 Executive Dashboard

#### Key Performance Indicators

| KPI | Target | Data Source |
|-----|--------|-------------|
| System Availability | 99.9% | CloudWatch |
| Average Response Time | <500ms | Prometheus |
| Error Rate | <0.1% | Prometheus |
| Daily Transaction Volume | Baseline | Custom Metrics |
| Daily Payment Volume | Baseline | Custom Metrics |
| Active Users | Baseline | Custom Metrics |

#### Grafana Dashboard JSON

```json
{
  "dashboard": {
    "title": "CardDemo Executive Dashboard",
    "panels": [
      {
        "title": "System Availability (30 days)",
        "type": "stat",
        "targets": [
          {
            "expr": "avg_over_time(up{job=~\"carddemo-.*\"}[30d]) * 100"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "thresholds": {
              "steps": [
                {"color": "red", "value": 99},
                {"color": "yellow", "value": 99.5},
                {"color": "green", "value": 99.9}
              ]
            }
          }
        }
      },
      {
        "title": "Transaction Volume (24h)",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(carddemo_transactions_total[24h]))"
          }
        ]
      },
      {
        "title": "Payment Success Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(rate(carddemo_payments_total{status=\"COMPLETED\"}[24h])) / sum(rate(carddemo_payments_total[24h])) * 100"
          }
        ]
      },
      {
        "title": "Response Time Trend",
        "type": "timeseries",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "p95"
          },
          {
            "expr": "histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))",
            "legendFormat": "p50"
          }
        ]
      }
    ]
  }
}
```

### 6.2 Operations Dashboard

#### Service Health Panel

```json
{
  "title": "Service Health",
  "type": "table",
  "targets": [
    {
      "expr": "up{job=~\"carddemo-.*\"}",
      "format": "table",
      "instant": true
    }
  ],
  "transformations": [
    {
      "id": "organize",
      "options": {
        "renameByName": {
          "job": "Service",
          "Value": "Status"
        }
      }
    }
  ]
}
```

#### Error Rate by Service

```json
{
  "title": "Error Rate by Service",
  "type": "timeseries",
  "targets": [
    {
      "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application) * 100",
      "legendFormat": "{{ application }}"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "percent",
      "thresholds": {
        "steps": [
          {"color": "green", "value": 0},
          {"color": "yellow", "value": 1},
          {"color": "red", "value": 5}
        ]
      }
    }
  }
}
```

### 6.3 Business Metrics Dashboard

#### Transaction Analytics

```json
{
  "title": "Transaction Analytics",
  "panels": [
    {
      "title": "Transactions by Type",
      "type": "piechart",
      "targets": [
        {
          "expr": "sum(increase(carddemo_transactions_total[24h])) by (type)"
        }
      ]
    },
    {
      "title": "Transaction Amount Distribution",
      "type": "histogram",
      "targets": [
        {
          "expr": "carddemo_transactions_amount_bucket"
        }
      ]
    },
    {
      "title": "Hourly Transaction Volume",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum(rate(carddemo_transactions_total[1h])) * 3600"
        }
      ]
    }
  ]
}
```

### 6.4 Migration Comparison Dashboard

#### Mainframe vs Cloud Comparison

| Metric | Mainframe Baseline | Cloud Target | Current |
|--------|-------------------|--------------|---------|
| Transaction TPS | 100 | 150 | [Live] |
| Response Time p95 | 800ms | 500ms | [Live] |
| Daily Batch Duration | 4 hours | 1 hour | [Live] |
| Error Rate | 0.5% | 0.1% | [Live] |

---

## 7. Synthetic Monitoring

### 7.1 CloudWatch Synthetics Canaries

#### Login Flow Canary

```javascript
// login-canary.js
const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');

const flowBuilderBlueprint = async function () {
    let page = await synthetics.getPage();
    
    // Navigate to login page
    await synthetics.executeStep('navigateToLogin', async function () {
        await page.goto('https://carddemo.example.com/login', {
            waitUntil: 'networkidle0',
            timeout: 30000
        });
    });
    
    // Enter credentials
    await synthetics.executeStep('enterCredentials', async function () {
        await page.type('#username', process.env.TEST_USERNAME);
        await page.type('#password', process.env.TEST_PASSWORD);
    });
    
    // Submit login
    await synthetics.executeStep('submitLogin', async function () {
        await page.click('#login-button');
        await page.waitForNavigation({ waitUntil: 'networkidle0' });
    });
    
    // Verify dashboard loaded
    await synthetics.executeStep('verifyDashboard', async function () {
        await page.waitForSelector('#dashboard-metrics', { timeout: 10000 });
        log.info('Dashboard loaded successfully');
    });
};

exports.handler = async () => {
    return await flowBuilderBlueprint();
};
```

#### API Health Canary

```javascript
// api-health-canary.js
const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');

const apiCanaryBlueprint = async function () {
    const endpoints = [
        { name: 'Auth Health', url: '/api/v1/auth/health' },
        { name: 'Customer Health', url: '/api/v1/customers/health' },
        { name: 'Account Health', url: '/api/v1/accounts/health' },
        { name: 'Card Health', url: '/api/v1/cards/health' },
        { name: 'Transaction Health', url: '/api/v1/transactions/health' },
        { name: 'Payment Health', url: '/api/v1/payments/health' }
    ];
    
    for (const endpoint of endpoints) {
        await synthetics.executeStep(endpoint.name, async function () {
            const response = await makeRequest(endpoint.url);
            if (response.statusCode !== 200) {
                throw new Error(`${endpoint.name} returned ${response.statusCode}`);
            }
            log.info(`${endpoint.name}: OK`);
        });
    }
};

async function makeRequest(path) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'api.carddemo.example.com',
            port: 443,
            path: path,
            method: 'GET',
            timeout: 10000
        };
        
        const req = https.request(options, (res) => {
            resolve({ statusCode: res.statusCode });
        });
        
        req.on('error', reject);
        req.on('timeout', () => reject(new Error('Request timeout')));
        req.end();
    });
}

exports.handler = async () => {
    return await apiCanaryBlueprint();
};
```

### 7.2 Canary Schedule

| Canary | Frequency | Timeout | Regions |
|--------|-----------|---------|---------|
| Login Flow | 5 minutes | 60 seconds | us-east-1, us-west-2 |
| API Health | 1 minute | 30 seconds | us-east-1 |
| Transaction Flow | 15 minutes | 120 seconds | us-east-1 |
| Payment Flow | 15 minutes | 120 seconds | us-east-1 |

---

## 8. Incident Response

### 8.1 Incident Severity Matrix

| Severity | Impact | Examples | Response |
|----------|--------|----------|----------|
| SEV1 | Complete outage | All services down, data loss | All hands, 5 min response |
| SEV2 | Major degradation | Core service down, >50% users affected | On-call + backup, 15 min |
| SEV3 | Minor degradation | Single service degraded, <10% users | On-call, 1 hour |
| SEV4 | Minimal impact | Non-critical issue, workaround exists | Next business day |

### 8.2 Runbooks

#### Service Restart Runbook

```markdown
## Service Restart Procedure

### Prerequisites
- kubectl access to cluster
- AWS console access

### Steps

1. **Identify affected service**
   ```bash
   kubectl get pods -n carddemo-prod | grep -v Running
   ```

2. **Check pod logs**
   ```bash
   kubectl logs -n carddemo-prod <pod-name> --tail=100
   ```

3. **Check resource usage**
   ```bash
   kubectl top pods -n carddemo-prod
   ```

4. **Restart deployment**
   ```bash
   kubectl rollout restart deployment/<service-name> -n carddemo-prod
   ```

5. **Monitor rollout**
   ```bash
   kubectl rollout status deployment/<service-name> -n carddemo-prod
   ```

6. **Verify health**
   ```bash
   curl https://api.carddemo.example.com/api/v1/<service>/health
   ```

### Escalation
If service doesn't recover within 10 minutes, escalate to SEV1.
```

#### Database Connection Issues Runbook

```markdown
## Database Connection Issues

### Symptoms
- Connection pool exhausted errors
- Slow queries
- Connection timeouts

### Investigation

1. **Check RDS metrics**
   - CPU utilization
   - Connection count
   - Free memory

2. **Check connection pool**
   ```bash
   curl http://<service>:8080/actuator/metrics/hikaricp.connections.active
   curl http://<service>:8080/actuator/metrics/hikaricp.connections.pending
   ```

3. **Check for long-running queries**
   ```sql
   SELECT pid, now() - pg_stat_activity.query_start AS duration, query
   FROM pg_stat_activity
   WHERE state != 'idle'
   ORDER BY duration DESC
   LIMIT 10;
   ```

### Resolution

1. **Kill long-running queries**
   ```sql
   SELECT pg_terminate_backend(<pid>);
   ```

2. **Increase connection pool** (temporary)
   ```bash
   kubectl set env deployment/<service> SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=50
   ```

3. **Scale RDS if needed**
   - Modify instance class in AWS console
   - Schedule during maintenance window if possible
```

### 8.3 Post-Incident Review Template

```markdown
## Post-Incident Review

### Incident Summary
- **Incident ID:** INC-XXXX
- **Date/Time:** YYYY-MM-DD HH:MM UTC
- **Duration:** X hours Y minutes
- **Severity:** SEV1/2/3/4
- **Services Affected:** 

### Timeline
| Time (UTC) | Event |
|------------|-------|
| HH:MM | Alert triggered |
| HH:MM | On-call acknowledged |
| HH:MM | Root cause identified |
| HH:MM | Mitigation applied |
| HH:MM | Service restored |

### Root Cause
[Detailed description of what caused the incident]

### Impact
- Users affected: X
- Transactions impacted: Y
- Revenue impact: $Z

### What Went Well
- 
- 

### What Could Be Improved
- 
- 

### Action Items
| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| | | | |

### Lessons Learned
[Key takeaways for preventing similar incidents]
```

---

## 9. Capacity Planning

### 9.1 Resource Baseline

| Service | CPU Request | CPU Limit | Memory Request | Memory Limit | Replicas |
|---------|-------------|-----------|----------------|--------------|----------|
| api-gateway | 500m | 1000m | 512Mi | 1Gi | 3 |
| auth-service | 250m | 500m | 256Mi | 512Mi | 2 |
| customer-service | 250m | 500m | 256Mi | 512Mi | 2 |
| account-service | 250m | 500m | 256Mi | 512Mi | 2 |
| card-service | 250m | 500m | 256Mi | 512Mi | 2 |
| transaction-service | 500m | 1000m | 512Mi | 1Gi | 3 |
| payment-service | 500m | 1000m | 512Mi | 1Gi | 3 |
| reporting-service | 250m | 500m | 512Mi | 1Gi | 2 |
| batch-service | 1000m | 2000m | 1Gi | 2Gi | 1 |

### 9.2 Auto-Scaling Configuration

```yaml
# HorizontalPodAutoscaler for transaction-service
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: transaction-service-hpa
  namespace: carddemo-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: transaction-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: 100
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 120
```

### 9.3 Growth Projections

| Metric | Current | 6 Months | 12 Months | Action Threshold |
|--------|---------|----------|-----------|------------------|
| Daily Transactions | 100K | 150K | 250K | 200K |
| Peak TPS | 50 | 75 | 125 | 100 |
| Database Size | 50GB | 100GB | 200GB | 150GB |
| Active Users | 10K | 15K | 25K | 20K |

---

## 10. Cost Monitoring

### 10.1 AWS Cost Allocation Tags

| Tag Key | Tag Values | Purpose |
|---------|------------|---------|
| Project | carddemo | Project identification |
| Environment | prod, staging, dev | Environment separation |
| Service | auth, customer, account, etc. | Service-level costs |
| Team | platform, backend, frontend | Team attribution |
| CostCenter | CC-12345 | Finance tracking |

### 10.2 Cost Alerts

```yaml
# AWS Budgets configuration
Budgets:
  - BudgetName: carddemo-monthly
    BudgetLimit:
      Amount: 10000
      Unit: USD
    BudgetType: COST
    TimeUnit: MONTHLY
    CostFilters:
      TagKeyValue:
        - "user:Project$carddemo"
    NotificationsWithSubscribers:
      - Notification:
          NotificationType: ACTUAL
          ComparisonOperator: GREATER_THAN
          Threshold: 80
        Subscribers:
          - SubscriptionType: EMAIL
            Address: carddemo-team@example.com
      - Notification:
          NotificationType: FORECASTED
          ComparisonOperator: GREATER_THAN
          Threshold: 100
        Subscribers:
          - SubscriptionType: EMAIL
            Address: carddemo-team@example.com
```

### 10.3 Cost Optimization Recommendations

| Area | Current | Recommended | Savings |
|------|---------|-------------|---------|
| RDS Instance | db.r5.xlarge | db.r5.large (off-peak) | 30% |
| EKS Nodes | On-Demand | Spot (non-critical) | 60% |
| Log Retention | 90 days all | Tiered retention | 40% |
| Data Transfer | Standard | VPC Endpoints | 20% |

---

## Appendix A: Monitoring Checklist

### Pre-Production Checklist

- [ ] All services expose /actuator/health endpoint
- [ ] All services expose /actuator/prometheus endpoint
- [ ] Structured logging configured
- [ ] X-Ray tracing enabled
- [ ] CloudWatch log groups created
- [ ] Prometheus ServiceMonitors configured
- [ ] Grafana dashboards imported
- [ ] AlertManager rules configured
- [ ] PagerDuty integration tested
- [ ] Slack integration tested
- [ ] Synthetic canaries deployed
- [ ] Runbooks documented
- [ ] On-call rotation established

### Post-Deployment Checklist

- [ ] All health checks passing
- [ ] Metrics flowing to Prometheus
- [ ] Logs appearing in CloudWatch
- [ ] Traces visible in X-Ray
- [ ] Dashboards showing data
- [ ] Test alerts triggered and received
- [ ] Canaries running successfully

---

## Appendix B: Tool Access and URLs

| Tool | URL | Access |
|------|-----|--------|
| Grafana | https://grafana.carddemo.example.com | SSO |
| Prometheus | https://prometheus.carddemo.example.com | VPN |
| AlertManager | https://alertmanager.carddemo.example.com | VPN |
| Kibana | https://kibana.carddemo.example.com | SSO |
| AWS Console | https://console.aws.amazon.com | IAM |
| X-Ray Console | https://console.aws.amazon.com/xray | IAM |
| CloudWatch | https://console.aws.amazon.com/cloudwatch | IAM |
| PagerDuty | https://carddemo.pagerduty.com | SSO |

---

*This monitoring guide should be reviewed and updated quarterly to ensure alignment with operational requirements and technology changes.*
