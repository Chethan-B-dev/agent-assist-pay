# PayNow Agent Assist System

Production-grade Spring Boot multi-module payment system with agentic AI assistance for payment decisions.

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │
                        │   (Port 8089)   │
                        └────────┬────────┘
                                 │
         ┌─────────────┬─────────┴────────┬─────────────┐
         │             │                  │             │
┌─────────────────┐    │    ┌──────────────────┐    ┌─────────────────┐
│   Payments      │    │    │    Accounts      │    │      Risk       │
│   Service       │◄───┼───►│    Service       │    │    Service      │
│   (Port 8080)   │    │    │   (Port 8081)    │    │  (Port 8082)    │
└─────────────────┘    │    └──────────────────┘    └─────────────────┘
│             │              │                       │
│             │    ┌─────────────────┐               │
└─────────────┼───►│      Case       │◄──────────────┘
│    │    Service      │
│    │  (Port 8083)    │
│    └─────────────────┘
│             │
┌────────┴────────────────────────┐
│           Redis                 │    ELK Stack
│     (Rate Limiting +            │  (Elasticsearch +
│     Idempotency Cache)          │   Logstash + Kibana)
└─────────────────────────────────┘
```

## Quick Start

1. **Build and run everything:**
   ```bash
   mvn clean package
   docker-compose up --build
   ```

2. **Test the API:**
   ```bash
   curl -X POST http://localhost:8089/payments/decide \
     -H "Content-Type: application/json" \
     -H "X-API-Key: payment-api-key" \
     -d '{
       "customerId": "c_123",
       "amount": 125.50,
       "currency": "USD", 
       "payeeId": "p_789",
       "idempotencyKey": "uuid-1"
     }'
   ```

## Key Features

- **Agent Orchestration**: AI agent plans and calls tools (getBalance, getRiskSignals, createCase)
- **Security**: API key authentication, PII redaction in logs
- **Rate Limiting**: Token bucket (5 req/sec per customer) via Redis
- **Idempotency**: Redis-backed duplicate request handling
- **Observability**: Structured JSON logs, metrics, ELK stack integration
- **Concurrency Safety**: Transactional balance reservations

## Services

- **API Gateway**: Centralized entry point for all services with security and rate limiting
- **Payments Service**: Main API with agent orchestration
- **Accounts Service**: H2 database for balance management
- **Risk Service**: Stubbed risk assessment logic
- **Case Service**: H2 database for review/block cases

## Optimizations

- **Latency**: Parallel tool execution, Redis caching, connection pooling
- **Security**: Defense-in-depth, PII redaction, API key auth
- **Observability**: Request correlation IDs, structured logging, metrics
- **Concurrency**: Serializable transaction isolation, pessimistic locking for critical operations

## Trade-offs

- **Redis vs In-Memory**: Chose Redis for scalability over simplicity
- **H2 vs PostgreSQL**: H2 for easy setup, would use PostgreSQL in production
- **Sync vs Async**: Synchronous for consistency, async event publishing
- **Token Bucket vs Fixed Window**: Chose token bucket for better handling of burst traffic
- **Gateway vs Direct Access**: Added gateway for centralized security at the cost of additional network hop

Access Kibana at http://localhost:5601 for log analysis.