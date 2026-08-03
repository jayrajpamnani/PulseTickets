# Event Ticketing Platform

Cloud-native event-ticketing platform built with Java 21, Spring Boot, Angular, PostgreSQL, Kafka, Docker, Kubernetes, and AWS.

## Local quick start

1. Copy `.env.example` to `.env` and set `JWT_SECRET` to a base64-encoded 32-byte value.
2. Run `docker compose up --build`.
3. Open `http://localhost:5173`. The gateway is at `http://localhost:8080`.

The compose stack runs PostgreSQL (one database per service), Kafka, the configuration server, gateway, user, event, ticket, payment, notification, analytics, and the web app.

## Architecture

```text
Angular -> API Gateway -> user | event | ticket | payment services
                         |      |      |
                     PostgreSQL per service
ticket/payment -> Kafka -> notification + analytics
event-banner upload -> S3 -> Lambda -> DynamoDB -> SNS
```

Each service exposes `/actuator/health` and `/actuator/prometheus`.

## AWS deployment

Infrastructure lives in `infra/terraform`. It provisions an EKS cluster, ECR repositories, RDS PostgreSQL instances, MSK Serverless, S3, DynamoDB, Lambda, SNS, and IAM roles. Run `terraform init`, `terraform plan`, and `terraform apply` only with an authenticated AWS CLI session and a reviewed variables file. See `infra/terraform/README.md`.

## API summary

Public: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/events`, `GET /api/events/{id}`.

Authenticated customers can view `/api/users/me`, reserve/list/cancel tickets, pay for their own reservations, and read browsing history. Admins manage events and can list users/all reservations. Requests use `Authorization: Bearer <token>`.
