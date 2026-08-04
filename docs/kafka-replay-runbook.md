# Kafka Consumer & Event Replay Runbook

This runbook outlines operational procedures for managing Kafka topics, resetting consumer group offsets, replaying events, and handling Dead Letter Queues (DLQ) in PulseTickets.

## 1. Overview of Topics & Consumer Groups

| Topic | Publisher | Consumer Group | Consumer Service |
| :--- | :--- | :--- | :--- |
| `reservation-created` | `ticket-service` | `notification` | `notification-service` |
| `reservation-created` | `ticket-service` | `analytics` | `analytics-service` |
| `payment-completed` | `payment-service` | `notification` | `notification-service` |
| `reservation-created.DLT` | Kafka Error Handler | DLQ Monitor | Manual Inspection |
| `payment-completed.DLT` | Kafka Error Handler | DLQ Monitor | Manual Inspection |

---

## 2. Inspecting Kafka Consumer Groups & Lag

To check consumer group status and unconsumed message count (lag):

```bash
# Exec into a Kafka pod or use local kafka-consumer-groups tool
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group analytics
```

---

## 3. Replaying Events (Offset Reset Procedure)

When a consumer service crashes or experiences a bug that requires reprocessing historical messages:

### Step 1: Scale Down Consumer Service
Before resetting offsets, scale down the worker deployment so it doesn't commit offsets concurrently:
```bash
kubectl scale deployment notification-service --replicas=0 -n pulse-tickets
```

### Step 2: Reset Consumer Group Offsets

**Option A: Reset to Earliest (Reprocess All Events)**
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification \
  --topic reservation-created \
  --reset-offsets --to-earliest --execute
```

**Option B: Shift Offsets Back by N Messages**
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification \
  --topic reservation-created \
  --reset-offsets --offset-by -100 --execute
```

**Option C: Reset to Specific Timestamp (YYYY-MM-THH:mm:ss.sss)**
```bash
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification \
  --topic reservation-created \
  --reset-offsets --to-datetime 2026-08-01T00:00:00.000 --execute
```

### Step 3: Scale Consumer Service Back Up
```bash
kubectl scale deployment notification-service --replicas=1 -n pulse-tickets
```

---

## 4. Idempotency Safety During Replays

The `notification-service` and `analytics-service` use database-backed idempotency tables (`processed_messages`). 
- If replaying events that have already been processed, the services will safely ignore duplicates based on the persisted message key.
- If you intend to forcibly re-trigger processing for historical messages during replay, truncate or clear the idempotency table:
```sql
TRUNCATE TABLE processed_messages;
```

---

## 5. Dead Letter Queue (DLQ) Recovery

Failed messages are routed to `<topic>.DLT` after 2 retries (configured via `DeadLetterPublishingRecoverer`).

### Inspecting Messages in Dead Letter Topics
```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic reservation-created.DLT \
  --from-beginning --max-messages 10
```

### Replaying Dead-Lettered Messages back to Main Topic
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic reservation-created.DLT --from-beginning | \
  kafka-console-producer --bootstrap-server localhost:9092 --topic reservation-created
```
