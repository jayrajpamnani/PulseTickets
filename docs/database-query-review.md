# Database Query Review & Tuning Guide

This document records recommended `EXPLAIN ANALYZE` commands, database query patterns, and indexing strategies for the PulseTickets microservices platform.

## 1. User Service Queries (`users` database)

### User Authentication Lookup
```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE username = 'alice';
```
- **Target Index**: `idx_users_username` (`UNIQUE INDEX (username)`)
- **Expected Execution Plan**: Index Scan using `idx_users_username` (Cost ~0.15..8.17, Rows=1).

### User Duplicate Check (Register)
```sql
EXPLAIN ANALYZE SELECT EXISTS (
    SELECT 1 FROM users WHERE username = 'alice' OR email = 'alice@example.com'
);
```
- **Target Index**: `idx_users_username`, `idx_users_email`
- **Expected Execution Plan**: Bitmap Or / Parallel Index Scan on `username` and `email`.

---

## 2. Event Service Queries (`events` database)

### Event Search & Pagination
```sql
EXPLAIN ANALYZE SELECT * FROM events 
WHERE LOWER(title) LIKE '%concert%' OR LOWER(venue) LIKE '%arena%' 
ORDER BY starts_at ASC 
LIMIT 20 OFFSET 0;
```
- **Target Index**: `idx_events_starts_at` (`INDEX (starts_at)`), GIN index on `title` / `venue` if full-text search is required under heavy load.
- **Expected Execution Plan**: Index Scan using `idx_events_starts_at` with Filter.

---

## 3. Ticket Service Queries (`tickets` database)

### User Reservation History
```sql
EXPLAIN ANALYZE SELECT * FROM reservations 
WHERE username = 'alice' 
ORDER BY created_at DESC;
```
- **Target Index**: `idx_reservations_user_created` (`INDEX (username, created_at DESC)`)
- **Expected Execution Plan**: Index Scan backward on `idx_reservations_user_created` (Cost ~0.28..12.45).

---

## 4. Payment Service Queries (`payments` database)

### Payment Lookup by Reservation ID
```sql
EXPLAIN ANALYZE SELECT * FROM payments WHERE reservation_id = 101;
```
- **Target Index**: `idx_payments_reservation_id` (`UNIQUE INDEX (reservation_id)`)
- **Expected Execution Plan**: Index Scan using `idx_payments_reservation_id` (Cost ~0.15..8.17).

---

## 5. Worker Service Idempotency Queries (`processed_messages`)

### Idempotency Key Lookup
```sql
EXPLAIN ANALYZE SELECT EXISTS (
    SELECT 1 FROM processed_messages WHERE message_key = 'reservation:reservation-created:0:42'
);
```
- **Target Index**: Primary Key (`message_key`)
- **Expected Execution Plan**: Primary Key Lookup (Cost ~0.15..8.17).
