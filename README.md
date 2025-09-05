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

## Refer notes.md for more details on implementation.

## Quick Start

1. **Build and run everything:**
   ```bash
   mvn clean package
   docker-compose up --build
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

**Test the API:**
   ```bash
   # example of an allowed decision
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
# Sample Response
  ```JSON
   {
  "decision": "allow",
  "reasons": [
    "low_risk_transaction"
  ],
  "agentTrace": [
    {
      "step": "plan",
      "detail": "Analyzing payment: 125.50 USD - will check balance, risk signals, and apply decision rules",
      "timestamp": "2025-09-05T18:22:34.312434595Z",
      "durationMs": null
    },
    {
      "step": "tool:getRiskSignals",
      "detail": "riskScore=29, level=LOW, factors=[high_velocity=multiple recent transactions(weight:5)]",
      "timestamp": "2025-09-05T18:22:34.451274720Z",
      "durationMs": 137
    },
    {
      "step": "tool:getBalance",
      "detail": "balance=1000.00, available=1000.00, status=ACTIVE",
      "timestamp": "2025-09-05T18:22:34.533791970Z",
      "durationMs": 220
    },
    {
      "step": "plan",
      "detail": "Evaluating decision factors",
      "timestamp": "2025-09-05T18:22:34.534004595Z",
      "durationMs": null
    },
    {
      "step": "decision",
      "detail": "Low risk transaction - ALLOW",
      "timestamp": "2025-09-05T18:22:34.534024095Z",
      "durationMs": null
    },
    {
      "step": "tool:reserveBalance",
      "detail": "reserved=125.50",
      "timestamp": "2025-09-05T18:22:34.579952720Z",
      "durationMs": 45
    },
    {
      "step": "decision",
      "detail": "Final decision: ALLOW based on 1 factors",
      "timestamp": "2025-09-05T18:22:34.579971970Z",
      "durationMs": null
    }
  ],
  "requestId": "req_a2ea3fad4a46"
}
```

```bash
# example of a blocked decision
curl -s -X POST http://localhost:8089/payments/decide \                                                                    󰠅 Development ☸ aks5-cluster-eastus-dev 
  -H "Content-Type: application/json" \
  -H "X-API-Key: payment-api-key" \
  -H "Accept: application/json" \
  -d '{
    "customerId": "c_123",
    "amount": 125455.50,
    "currency": "USD", 
    "payeeId": "p_789",
    "idempotencyKey": "uuid-'$(date +%s)'"
  }' | jq
```
# Sample Response
```JSON
{
  "decision": "block",
  "reasons": [
    "insufficient_funds"
  ],
  "agentTrace": [
    {
      "step": "plan",
      "detail": "Analyzing payment: 125455.50 USD - will check balance, risk signals, and apply decision rules",
      "timestamp": "2025-09-05T18:27:16.011466794Z",
      "durationMs": null
    },
    {
      "step": "tool:getRiskSignals",
      "detail": "riskScore=63, level=HIGH, factors=[high_amount=amount > 1000(weight:9)]",
      "timestamp": "2025-09-05T18:27:16.041577253Z",
      "durationMs": 29
    },
    {
      "step": "tool:getBalance",
      "detail": "balance=1000.00, available=874.50, status=ACTIVE",
      "timestamp": "2025-09-05T18:27:16.042158378Z",
      "durationMs": 30
    },
    {
      "step": "plan",
      "detail": "Evaluating decision factors",
      "timestamp": "2025-09-05T18:27:16.042330919Z",
      "durationMs": null
    },
    {
      "step": "decision",
      "detail": "Insufficient funds - BLOCK",
      "timestamp": "2025-09-05T18:27:16.042350794Z",
      "durationMs": null
    },
    {
      "step": "tool:createCase",
      "detail": "Created block case with ID: case_592a5da12c05",
      "timestamp": "2025-09-05T18:27:16.196646669Z",
      "durationMs": 154
    },
    {
      "step": "decision",
      "detail": "Final decision: BLOCK based on 1 factors",
      "timestamp": "2025-09-05T18:27:16.196668128Z",
      "durationMs": null
    }
  ],
  "requestId": "req_592a5da12c05"
}
```