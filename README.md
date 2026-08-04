# PulseTickets

PulseTickets is a cloud-native event-ticketing platform built with Java 17, Spring Boot, Angular, PostgreSQL, Kafka, Docker, Kubernetes, and AWS.

Customers can browse events, register or sign in, reserve tickets, and record payments. Administrators can create events and upload event banners.

## High-level architecture

```mermaid
flowchart TB
    Browser[Customer or administrator browser]

    subgraph Edge[Frontend and API delivery]
        WebCDN[CloudFront distribution<br/>static frontend and /api proxy]
        WebS3[(S3 bucket<br/>Angular production build)]
        LoadBalancer[Internet-facing load balancer]
        WebCDN -->|static Angular assets| WebS3
        WebCDN -->|/api/*| LoadBalancer
    end

    Browser -->|HTTPS| WebCDN

    subgraph AWS[AWS production platform]
        subgraph EKS[EKS cluster]
            Gateway[API Gateway<br/>routing, CORS, JWT validation]
            Config[Config Server]
            User[User Service]
            Event[Event Service]
            Ticket[Ticket Service]
            Payment[Payment Service]
            Notification[Notification Service]
            Analytics[Analytics Service]

            Gateway --> User
            Gateway --> Event
            Gateway --> Ticket
            Gateway --> Payment
            Config -. configuration .-> Gateway
            Config -. configuration .-> User
            Config -. configuration .-> Event
            Config -. configuration .-> Ticket
            Config -. configuration .-> Payment
            Config -. configuration .-> Notification
            Config -. configuration .-> Analytics
        end

        subgraph Data[Service-owned data and messaging]
            UsersDB[(RDS PostgreSQL<br/>users)]
            EventsDB[(RDS PostgreSQL<br/>events)]
            TicketsDB[(RDS PostgreSQL<br/>tickets)]
            PaymentsDB[(RDS PostgreSQL<br/>payments)]
            Kafka[(Amazon MSK Kafka)]
        end

        subgraph Banners[Event-banner upload and delivery]
            BannerS3[(Private S3<br/>banner bucket)]
            BannerLambda[Banner Processor Lambda]
            Metadata[(DynamoDB<br/>banner metadata)]
            SNS[SNS notification]
            BannerCDN[CloudFront<br/>banner distribution]
        end

        Secrets[Secrets Manager and IAM]
        ECR[ECR container images]
        Prometheus[Prometheus]
        CloudWatch[CloudWatch]
    end

    LoadBalancer --> Gateway
    User --> UsersDB
    Event --> EventsDB
    Ticket --> TicketsDB
    Payment --> PaymentsDB
    Ticket -->|reservation events| Kafka
    Payment -->|payment events| Kafka
    Kafka -->|consume| Notification
    Kafka -->|consume| Analytics

    Event -->|pre-signed upload URL| Browser
    Browser -->|direct upload| BannerS3
    BannerS3 -->|object-created event| BannerLambda
    BannerLambda --> Metadata
    BannerLambda --> SNS
    BannerS3 --> BannerCDN
    Browser -->|banner requests| BannerCDN

    Secrets -. secrets and permissions .-> EKS
    ECR -. deployment images .-> EKS
    EKS -. actuator metrics .-> Prometheus
    EKS -. logs .-> CloudWatch
    Kafka -. broker logs .-> CloudWatch
    BannerLambda -. function logs .-> CloudWatch

    subgraph Local[Local development: Docker Compose]
        Compose[Angular web, gateway, Spring Boot services,<br/>Kafka, four PostgreSQL databases, and Prometheus]
    end
```

In production, CloudFront serves the Angular application from S3 and forwards `/api/*` requests to the internet-facing load balancer and API Gateway in EKS. Each main business service owns its PostgreSQL database. Kafka handles asynchronous reservation and payment events, while banner uploads go directly from an administrator's browser to S3 using a pre-signed URL, then trigger Lambda, DynamoDB, and SNS processing. Docker Compose provides the local equivalent of the application and data layers.

## Services

| Component | Responsibility |
|---|---|
| `web` | Angular customer and administrator interface |
| `api-gateway` | Single API entry point, routing, CORS, and JWT validation |
| `user-service` | Registration, login, profiles, roles, and JWT issuing |
| `event-service` | Event catalog, search, inventory, and banner URLs |
| `ticket-service` | Reservations, cancellations, and inventory coordination |
| `payment-service` | Payment records and payment events |
| `notification-service` | Kafka-based reservation and payment notifications |
| `analytics-service` | Kafka-based reservation metrics |
| `config-server` | Centralized Spring configuration |
| `banner-processor` | AWS Lambda handler for uploaded banner metadata |

## Run locally

Set the required environment variables in a `.env` file, including `JWT_SECRET`, then run:

```bash
docker compose up --build
```

Open the application at [http://localhost:4200](http://localhost:4200). The API Gateway is available at [http://localhost:8080](http://localhost:8080), and Prometheus is available at [http://localhost:9090](http://localhost:9090).

For Angular development outside Docker:

```bash
cd web
npm install
npm start
```

The local Compose stack includes Kafka, four PostgreSQL databases, the backend services, the gateway, the configuration server, the frontend, and Prometheus.

## Database schemas

Flyway migrations are stored inside each service:

- `user-service`: `users`
- `event-service`: `events`
- `ticket-service`: `reservations`
- `payment-service`: `payments`

The notification and analytics workers use `processed_messages` for Kafka idempotency. Their local configuration uses in-memory H2 databases.

## API summary

Public endpoints:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/events`
- `GET /api/events/{id}`

Authenticated endpoints support profiles, reservations, cancellations, and payments. Administrator-only endpoints support event creation, updates, deletion, and banner uploads. Authenticated requests use `Authorization: Bearer <token>`.

## AWS and production deployment

Terraform files are in [`infra/terraform`](infra/terraform). The production design uses:

- **EKS and ECR** for Kubernetes workloads and container images.
- **RDS PostgreSQL** for service databases.
- **MSK** for managed Kafka.
- **S3 and CloudFront** for frontend and banner storage and delivery.
- **Lambda, DynamoDB, and SNS** for banner processing and notifications.
- **Secrets Manager and IAM** for secrets and service permissions.
- **CloudWatch Logs** for AWS and Kafka logs.

The deployment script is [`scripts/deploy-production.sh`](scripts/deploy-production.sh). Review Terraform variables and the plan before applying infrastructure.

## Monitoring

All Spring Boot services expose:

- `/actuator/health`
- `/actuator/prometheus`

Prometheus configuration and alert rules are in [`monitoring`](monitoring). Operational notes are in [`docs`](docs).
