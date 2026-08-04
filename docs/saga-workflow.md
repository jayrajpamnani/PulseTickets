# Reservation/payment compensation workflow

The platform uses an asynchronous, compensating workflow rather than a cross-database transaction:

1. `ticket-service` asks `event-service` to reserve inventory. If that call fails, no reservation is persisted.
2. The reservation is persisted locally and publishes `reservation-created` with the reservation ID as the Kafka key.
3. Payment is persisted independently and publishes `payment-completed` with the same reservation ID.
4. If a customer cancels before payment, ticket service marks the reservation `CANCELLED` and calls event service to release inventory.
5. If a downstream Kafka consumer fails, Spring Kafka retries twice and publishes the record to the topic dead-letter suffix for replay/inspection.

The reservation ID is the workflow correlation ID. Consumers are idempotent for a Kafka record partition/offset, and payment creation is protected by the unique reservation ID constraint.
