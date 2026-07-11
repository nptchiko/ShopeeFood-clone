# Event-Driven Architecture (Kafka) — Software Requirements Specification
---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [Event Catalogue](#3-event-catalogue)
4. [Topic Configuration](#4-topic-configuration)
5. [Functional Requirements](#5-functional-requirements)
6. [Flow Diagrams](#6-flow-diagrams)
7. [Business Rules](#7-business-rules)
8. [Error Handling & Dead-Letter Topics](#8-error-handling--dead-letter-topics)
9. [Infrastructure Configuration](#9-infrastructure-configuration)
10. [Non-Functional Requirements](#10-non-functional-requirements)

---

## 1. Introduction

### 1.1 Purpose
This document defines the software requirements for the **Event-Driven Architecture (EDA)** module of the ShopeeFood Clone backend. It specifies the Kafka topic design, domain event contracts, producer and consumer behaviour, error handling strategy, and all reliability guarantees.

### 1.2 Scope
The module covers all asynchronous event flows triggered by the **Authentication** domain, as well as the notification delivery infrastructure that consumes those events.

| Sub-feature              | Responsibility                                                              |
|---|---|
| **Event Publishing**      | Produce domain events to Kafka topics after state changes                  |
| **Notification Consumer** | Consume events and dispatch email notifications via SMTP                   |
| **OTP Delivery**          | Async dispatch of 6-digit OTP emails (independent of registration)         |
| **Welcome Email Delivery**| Async dispatch of greeting-only email on registration (no OTP embedded)    |
| **Reliability**           | At-least-once delivery via manual ACK + retry + DLT                        |
| **Dead-Letter Handling**  | Route poison-pill messages to DLT after max retries                        |
| **Observability**         | Correlation ID propagation across all event boundaries                      |

### 1.3 Technology Stack

| Layer             | Technology                                                   |
|---|---|
| Message Broker    | Apache Kafka (KRaft mode in Docker)                         |
| Spring Integration| Spring Boot 4.1.0 + Spring Kafka                            |
| Serialization     | Jackson JSON (JacksonJsonSerializer / JacksonJsonDeserializer) |
| Type Resolution   | Kafka type headers (spring.json.add.type.headers: true)     |
| Error Handling    | ErrorHandlingDeserializer + DefaultErrorHandler + DLT       |
| Notification      | SMTP via JavaMailSender (Spring Mail)                       |
| Correlation       | UUID v4 correlationId per event                             |
| Configuration     | application-common.yaml (profile-grouped)                   |

---

## 2. Overall Description

### 2.1 Architectural Pattern

The system adopts a **producer-consumer** event-driven pattern where domain services publish immutable events to Kafka topics and notification consumers independently react without coupling to the originating domain.

OTP delivery and welcome email delivery are now **fully independent event streams**. Registration triggers both, but they travel through separate topics, separate producers, and are consumed independently.

```
AuthService                 Kafka Broker              KafkaNotificationConsumer
    |                            |                            |
    |-- publishUserRegistered -> [user.registered] --------> handleUserRegistered()
    |                            |                            |-- sendEmail() (greeting only)
    |                            |                            |
    |-- generateAndSendOtp() --> UserOtpService              |
                                    |                         |
                              publishOtpVerificationRequested |
                                    |                         |
                                    v                         |
                              [otp.verification.requested] -> handleOtpVerificationRequested()
                                                              |-- sendOtpEmail() (OTP only)
                                                              |
                             [*.DLT] (on failure for either stream)
```

### 2.2 Key Design Decisions

| Decision                             | Rationale                                                                          |
|---|---|
| **Detached OTP from registration**   | OTP sending is delegated to UserOtpService, which publishes its own event independently. AuthService only publishes the UserRegisteredEvent. This allows OTP resend to reuse the exact same code path without special-casing the registration scenario. |
| **Welcome email contains no OTP**    | UserRegisteredEvent carries no otpCode. The welcome email is a pure greeting. OTP is delivered via a separate dedicated email through OtpVerificationRequestedEvent. |
| **At-least-once delivery**           | Offset committed via manual ACK only on success — never lost even on crash          |
| **Email as partition key**           | All events for the same user land on the same partition; ordering guaranteed        |
| **Type headers enabled**             | spring.json.use.type.headers: true lets the deserializer resolve the exact event class without a mapping registry |
| **Dead-Letter Topics**               | Failed messages after 3 retries are routed to *.DLT for inspection/replay          |
| **Correlation ID per event**         | UUID generated at publish time, logged by both producer and consumer for end-to-end tracing |
| **Decoupled notification**           | Email delivery is entirely in KafkaNotificationConsumer — auth domain has zero SMTP dependency |

### 2.3 Who Publishes What

| Event                           | Published by       | Triggered from           |
|---|---|---|
| UserRegisteredEvent             | AuthService        | POST /api/auth/register  |
| OtpVerificationRequestedEvent   | UserOtpService     | POST /api/auth/register (via generateAndSendRegistrationOtp) AND POST /api/auth/otp/send |

### 2.4 Consumer Group

| Setting          | Value                              |
|---|---|
| Group ID         | shopee-food-notification-group     |
| Concurrency      | 3 threads (one per partition)      |
| Offset Commit    | Manual (MANUAL_IMMEDIATE)          |
| Auto-offset Reset| earliest                           |

---

## 3. Event Catalogue

### 3.1 OtpVerificationRequestedEvent

> **Topic:** otp.verification.requested
> **Schema version:** 1
> **Published by:** UserOtpService.generateAndSendRegistrationOtp()
> **Triggered by:** Registration flow (via AuthService) AND standalone OTP resend (POST /api/auth/otp/send)

Fired whenever the system generates a new OTP code — either on initial registration or on explicit OTP resend. This event is the **sole trigger** for OTP email delivery. It is independent of `UserRegisteredEvent`.

#### Schema

| Field         | Type             | Nullable | Description                                             |
|---|---|---|---|
| correlationId | UUID             | No       | Unique event ID for idempotency and distributed tracing |
| email         | String           | No       | Recipient email address; also used as Kafka partition key |
| otpCode       | String (6-digit) | No       | The OTP code to embed in the verification email         |
| requestedAt   | OffsetDateTime   | No       | UTC timestamp when the event was created                |

#### Example Payload (JSON)

```json
{
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "miku@example.com",
  "otpCode": "482931",
  "requestedAt": "2026-07-11T06:15:00Z"
}
```

---

### 3.2 UserRegisteredEvent

> **Topic:** user.registered
> **Schema version:** 1
> **Published by:** AuthService.register()
> **Triggered by:** POST /api/auth/register only

Fired when a new user account is successfully created. This event is **solely responsible for triggering the welcome/greeting email**. It does **not** carry an OTP code — OTP delivery is handled by the separate `OtpVerificationRequestedEvent`.

#### Schema

| Field         | Type           | Nullable | Description                                               |
|---|---|---|---|
| correlationId | UUID           | No       | Unique event ID for idempotency and distributed tracing   |
| userId        | UUID           | No       | The newly created user's primary key                      |
| email         | String         | No       | Registered email address; also used as Kafka partition key |
| displayName   | String         | No       | User's display name for personalised email greeting       |
| registeredAt  | OffsetDateTime | No       | UTC timestamp when the user account was created           |

#### Example Payload (JSON)

```json
{
  "correlationId": "f7e8d9c0-b1a2-3456-7890-abcdef123456",
  "userId": "d4e5f6a7-b8c9-0123-4567-890abcdef012",
  "email": "miku@example.com",
  "displayName": "Miku Nakano",
  "registeredAt": "2026-07-11T06:15:00Z"
}
```

---

## 4. Topic Configuration

### 4.1 Topics

| Topic Name                         | Partitions | Replicas           | Purpose                                               |
|---|---|---|---|
| otp.verification.requested         | 3          | 1 (dev) / 3 (prod) | OTP dispatch events (registration + resend)           |
| user.registered                    | 3          | 1 (dev) / 3 (prod) | New user registration welcome email events            |
| otp.verification.requested.DLT     | 1          | 1                  | Dead-letter for OTP events after max retries          |
| user.registered.DLT                | 1          | 1                  | Dead-letter for registration events after max retries |

### 4.2 Partition Key Strategy

All events use the user's **email address** as the Kafka message key. This guarantees that all events for the same user are written to the same partition, preserving ordering for a given user's event stream.

```
kafkaTemplate.send(TOPIC, user.getEmail(), event);
                          ^^^^^^^^^^^^^^^^
                          Partition key → consistent routing per user
```

### 4.3 Topic Provisioning

Topics are automatically created at application startup by Spring Kafka's `KafkaAdmin` bean declared in `KafkaTopicConfig`. The `fail-fast: false` admin setting prevents the app from crashing if Kafka is temporarily unavailable at boot.

---

## 5. Functional Requirements

### 5.1 UserRegisteredEvent Publishing

| ID    | Requirement |
|---|---|
| K-01  | The system **shall** publish a UserRegisteredEvent to user.registered upon successful user account creation in AuthService.register(). |
| K-02  | UserRegisteredEvent **shall** contain only: correlationId, userId, email, displayName, registeredAt. It **shall not** contain an OTP code. |
| K-03  | Each published event **shall** include a unique correlationId (UUID v4) generated at publish time. |
| K-04  | The user's email address **shall** be used as the Kafka message partition key. |
| K-05  | The producer **shall** embed a Java type header into every Kafka record for type-safe deserialization. |
| K-06  | On broker acknowledgment, the publisher **shall** log correlationId, userId, email, partition, and offset at DEBUG level. |
| K-07  | On broker send failure, the publisher **shall** log correlationId, userId, and error message at ERROR level without re-throwing. |

### 5.2 OtpVerificationRequestedEvent Publishing

| ID    | Requirement |
|---|---|
| K-08  | The system **shall** publish an OtpVerificationRequestedEvent to otp.verification.requested each time UserOtpService.generateAndSendRegistrationOtp() is called, regardless of whether it is triggered by registration or a standalone resend request. |
| K-09  | OtpVerificationRequestedEvent **shall** contain: correlationId, email, otpCode, requestedAt. |
| K-10  | On broker send failure, the publisher **shall** log correlationId, email, and error at ERROR level without re-throwing. |

### 5.3 Welcome Email Consumer (user.registered)

| ID    | Requirement |
|---|---|
| K-11  | The consumer **shall** listen on topic user.registered and dispatch a greeting-only welcome email upon receipt. |
| K-12  | The welcome email **shall** use subject "Welcome to ShopeeFood!" and include the user's displayName in the body. |
| K-13  | The welcome email **shall not** contain any OTP code. OTP is delivered through a separate consumer on a separate topic. |
| K-14  | The consumer **shall** call EmailService.sendEmail(email, subject, body) to deliver the welcome email. |
| K-15  | The consumer **shall** commit the offset (via Acknowledgment.acknowledge()) only after the email service call returns successfully. |
| K-16  | If EmailService.sendEmail() throws, the consumer **shall** re-throw the exception to allow the DefaultErrorHandler to apply the retry policy. |
| K-17  | The consumer **shall** log correlationId, userId, email, partition, and offset at INFO level upon receiving each event. |

### 5.4 OTP Email Consumer (otp.verification.requested)

| ID    | Requirement |
|---|---|
| K-18  | The consumer **shall** listen on topic otp.verification.requested and call EmailService.sendOtpEmail(email, otpCode) upon receipt. |
| K-19  | The consumer **shall** commit the offset only after the email service call returns successfully. |
| K-20  | If EmailService.sendOtpEmail() throws, the consumer **shall** re-throw the exception to allow the DefaultErrorHandler to apply the retry policy. |
| K-21  | The consumer **shall** log correlationId, email, partition, and offset at INFO level upon receiving each event. |

### 5.5 Error Recovery

| ID    | Requirement |
|---|---|
| K-22  | The system **shall** retry failed consumer processing up to **3 times** with a **2-second fixed backoff** before routing to the DLT. |
| K-23  | Deserialization errors **shall** be caught by ErrorHandlingDeserializer and routed directly to the DLT without triggering the retry policy. |
| K-24  | Messages routed to a DLT **shall** retain the original topic name, partition, offset, and error details in the Kafka record headers. |

---

## 6. Flow Diagrams

### 6.1 Registration — Two Independent Event Streams

On registration, `AuthService` triggers two **independent** side effects:
1. Publishes `UserRegisteredEvent` directly → triggers welcome email
2. Calls `UserOtpService.generateAndSendRegistrationOtp()` → which publishes `OtpVerificationRequestedEvent` → triggers OTP email

```
Client            AuthService           UserOtpService      KafkaEventPublisher   KafkaNotificationConsumer   EmailService
  |                   |                      |                      |                       |                      |
  |- POST /register ->|                      |                      |                       |                      |
  |                   |- userService.create()                       |                       |                      |
  |                   |                      |                      |                       |                      |
  |                   |---- publishUserRegistered(user) ----------->|                       |                      |
  |                   |                      |                      |-> [user.registered] ->|                      |
  |                   |                      |                      |                       |- sendEmail() ------> |
  |                   |                      |                      |                       |  (greeting only)     |
  |                   |                      |                      |                       |                      |
  |                   |- generateAndSendOtp(email) --------------->  |                       |                      |
  |                   |   (via UserOtpService)|                     |                       |                      |
  |                   |                      |- publishOtpVerification(email, otp) -------> |                      |
  |                   |                      |                      |-> [otp.verification.requested]               |
  |                   |                      |                      |                       |- sendOtpEmail() ---> |
  |                   |                      |                      |                       |  (OTP code only)     |
  |<-- 201 { user } --|                      |                      |                       |                      |
```


### 6.2 OTP Resend — Reuses Same Event Stream

OTP resend calls the exact same `UserOtpService.generateAndSendRegistrationOtp()` method, producing the same `OtpVerificationRequestedEvent`. No `UserRegisteredEvent` is published on resend.

```
Client            AuthService           UserOtpService      KafkaEventPublisher   KafkaNotificationConsumer   EmailService
  |                   |                      |                      |                       |                      |
  |- POST /otp/send ->|                      |                      |                       |                      |
  |                   |- generateAndSendOtp(email) --------------->  |                       |                      |
  |                   |                      |- publishOtpVerification(email, newOtp) -----> |                      |
  |                   |                      |                      |-> [otp.verification.requested]               |
  |                   |                      |                      |                       |- sendOtpEmail() ---> |
  |<-- 200 OK --------|                      |                      |                       |                      |
```

### 6.3 Error Handling & DLT Flow

Applies identically to both consumer listeners.

```
KafkaNotificationConsumer           DefaultErrorHandler         Dead-Letter Topic
          |                                 |                          |
          |- EmailService throws            |                          |
          |- re-throw --------------------> |                          |
          |                                 |- retry (attempt 1, +2s) |
          |- EmailService throws again      |                          |
          |- re-throw --------------------> |                          |
          |                                 |- retry (attempt 2, +2s) |
          |- EmailService throws again      |                          |
          |- re-throw --------------------> |                          |
          |                                 |- retry (attempt 3, +2s) |
          |- EmailService throws again      |                          |
          |                                 |- max retries exceeded   |
          |                                 |- DeadLetterPublishingRecoverer -> [*.DLT]
```

### 6.4 Event Tracing by Correlation ID

Each event has its own independent correlation ID. Tracing a full registration looks like:

```
-- Stream 1: Welcome email (UserRegisteredEvent) --
[PUBLISH]   [Kafka] Publishing UserRegisteredEvent  | correlationId=aaa-111 | userId=... | email=miku@example.com
[BROKER]    [Kafka] UserRegisteredEvent sent         | correlationId=aaa-111 | partition=1 | offset=42
[CONSUME]   [Kafka] Received UserRegisteredEvent     | correlationId=aaa-111 | partition=1 | offset=42
[COMMIT]    [Kafka] UserRegisteredEvent processed    | correlationId=aaa-111

-- Stream 2: OTP email (OtpVerificationRequestedEvent) --
[PUBLISH]   [Kafka] Publishing OtpVerificationRequestedEvent | correlationId=bbb-222 | to=miku@example.com
[BROKER]    [Kafka] OtpVerificationRequestedEvent sent       | correlationId=bbb-222 | partition=1 | offset=15
[CONSUME]   [Kafka] Received OtpVerificationRequestedEvent   | correlationId=bbb-222 | partition=1 | offset=15
[COMMIT]    [Kafka] OtpVerificationRequestedEvent processed  | correlationId=bbb-222
```

---

## 7. Business Rules

| ID    | Rule |
|---|---|
| BR-01 | Each domain event is **immutable** once published. Consumers must never modify or re-publish a received event payload. |
| BR-02 | The correlationId is a UUID v4 generated at publish time and **must not** be reused across events. Each event (UserRegisteredEvent and OtpVerificationRequestedEvent) carries its **own independent** correlationId. |
| BR-03 | Consumers **must** commit offsets only after successfully completing all side effects (e.g., email dispatch). |
| BR-04 | The email field **must** be used as the Kafka message key — never null or an empty string. |
| BR-05 | UserRegisteredEvent **must not** carry an OTP code. Its sole purpose is to trigger the greeting email. |
| BR-06 | OTP delivery is the sole responsibility of OtpVerificationRequestedEvent, produced exclusively by UserOtpService. AuthService **must not** embed OTP codes in UserRegisteredEvent. |
| BR-07 | A registration triggers exactly **two** independent events on **two** separate topics. The welcome email and the OTP email are delivered independently with no ordering guarantee between them. |
| BR-08 | OTP resend publishes **only** OtpVerificationRequestedEvent — no UserRegisteredEvent is published. |
| BR-09 | Messages routed to a DLT **must** be treated as an alert condition and investigated; no automatic re-processing is implemented. |
| BR-10 | The Kafka producer **must** be configured with enable.idempotence=true and acks=all to guarantee exactly-once writes to the broker. |

---

## 8. Error Handling & Dead-Letter Topics

### 8.1 Retry Policy

| Setting        | Value  | Notes                                                          |
|---|---|---|
| Max Attempts   | 3      | Configured via FixedBackOff(2000, 3) in KafkaConsumerConfig   |
| Retry Interval | 2000ms | Fixed delay between attempts                                   |
| Recoverer      | DeadLetterPublishingRecoverer | Publishes to {topic}.DLT on exhaustion  |

### 8.2 Deserialization Error Strategy

`ErrorHandlingDeserializer` wraps `JacksonJsonDeserializer`. If a Kafka record cannot be deserialized (malformed JSON, unknown type), the error is captured and the record is forwarded to the DLT without invoking the listener — preventing an infinite retry loop on poison-pill messages.

### 8.3 DLT Header Reference

When a message is routed to a DLT, Spring Kafka attaches the following headers to the DLT record:

| Header                           | Content                          |
|---|---|
| kafka_dlt-original-topic         | Source topic name                |
| kafka_dlt-original-partition     | Source partition                 |
| kafka_dlt-original-offset        | Source offset                    |
| kafka_dlt-exception-message      | Exception message string         |
| kafka_dlt-exception-stacktrace   | Full exception stack trace       |

### 8.4 DLT Topic Map

| Original Topic                 | DLT                                 |
|---|---|
| otp.verification.requested     | otp.verification.requested.DLT      |
| user.registered                | user.registered.DLT                 |

---

## 9. Infrastructure Configuration

### 9.1 Producer Configuration (application-common.yaml)

| Property                        | Value                                       | Notes                                               |
|---|---|---|
| bootstrap-servers               | ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}   | Overridden via env var in Docker                    |
| key-serializer                  | StringSerializer                            | Email address key                                   |
| value-serializer                | JacksonJsonSerializer                       | JSON with type headers                              |
| acks                            | all                                         | Wait for all ISR replicas                           |
| retries                         | 3                                           | Producer-level retries on transient failures        |
| enable.idempotence              | true                                        | Exactly-once write to broker                        |
| spring.json.add.type.headers    | true                                        | Embeds __TypeId__ header for type-safe deserialization |
| reconnect.backoff.ms            | 1000                                        | 1s before first reconnect                           |
| reconnect.backoff.max.ms        | 30000                                       | Cap backoff at 30s                                  |
| max.block.ms                    | 5000                                        | Don't block send() more than 5s when broker is down |

### 9.2 Consumer Configuration (application-common.yaml)

| Property                                  | Value                               | Notes                                             |
|---|---|---|
| group-id                                  | shopee-food-notification-group      |                                                   |
| auto-offset-reset                         | earliest                            | Process from start if no committed offset         |
| enable-auto-commit                        | false                               | Manual ACK mode                                   |
| key-deserializer                          | StringDeserializer                  |                                                   |
| value-deserializer                        | ErrorHandlingDeserializer           | Wraps Jackson deserializer                        |
| spring.deserializer.value.delegate.class  | JacksonJsonDeserializer             |                                                   |
| spring.json.trusted.packages              | org.intern.shopeefoodclone.events   | Security: only trust own event classes            |
| spring.json.use.type.headers              | true                                | Resolves class from __TypeId__ header             |
| reconnect.backoff.ms                      | 1000                                |                                                   |
| default.api.timeout.ms                    | 10000                               |                                                   |

### 9.3 Admin Configuration

| Property              | Value | Notes                                           |
|---|---|---|
| fail-fast             | false | App starts even if Kafka is unavailable at boot |
| reconnect.backoff.ms  | 1000  |                                                 |
| request.timeout.ms    | 5000  |                                                 |

### 9.4 Kafka Component Map

| Class                           | Package           | Role                                               |
|---|---|---|
| KafkaTopicConfig                | config.kafka      | Declares topics and KafkaAdmin bean                |
| KafkaProducerConfig             | config.kafka      | Declares ProducerFactory and KafkaTemplate         |
| KafkaConsumerConfig             | config.kafka      | Declares ConsumerFactory and listener container factory |
| KafkaEventPublisher             | infras.messaging  | Domain-facing publisher; publishUserRegistered() and publishOtpVerificationRequested() |
| KafkaNotificationConsumer       | infras.messaging  | Kafka listener for both notification topics        |
| OtpVerificationRequestedEvent   | events            | Event POJO — OTP dispatch (no registration coupling) |
| UserRegisteredEvent             | events            | Event POJO — welcome greeting (no OTP field)       |
| UserOtpService                  | auth.otp          | Generates OTP + publishes OtpVerificationRequestedEvent |
| AuthService                     | auth              | Creates user + publishes UserRegisteredEvent; delegates OTP to UserOtpService |

---

## 10. Non-Functional Requirements

| ID     | Category       | Requirement |
|---|---|---|
| NFR-01 | Reliability    | All events **must** be delivered at least once. Offset is committed only after successful consumer processing. |
| NFR-02 | Reliability    | The producer **must** be idempotent (enable.idempotence=true, acks=all) to prevent duplicate writes caused by network retries. |
| NFR-03 | Resilience     | Consumer failures **must** be retried up to 3 times before routing to the DLT. The application **must not** crash on repeated consumer failure. |
| NFR-04 | Resilience     | Deserialization failures (poison-pill messages) **must** be routed directly to the DLT without retrying. |
| NFR-05 | Availability   | Kafka broker unavailability at application startup **must not** prevent the application from starting (fail-fast: false). |
| NFR-06 | Availability   | Email (SMTP) failures **should** trigger the retry + DLT flow and **must not** cause data loss or application crashes. |
| NFR-07 | Observability  | Every event **must** carry a UUID correlationId logged by both publisher and consumer. Because registration produces two events, each **must** carry its own independent correlationId. |
| NFR-08 | Observability  | Producer send confirmation (partition + offset) **must** be logged at DEBUG level. Consumer receipt and commit **must** be logged at INFO level. |
| NFR-09 | Ordering       | All events for a given user email **must** be written to the same Kafka partition (email as message key) to preserve per-user ordering within each topic. |
| NFR-10 | Decoupling     | AuthService **must not** be aware of OTP email delivery. OTP concerns are encapsulated entirely within UserOtpService and the otp.verification.requested topic. |
| NFR-11 | Security       | The spring.json.trusted.packages setting **must** be scoped to org.intern.shopeefoodclone.events to prevent deserialization of arbitrary classes. |
| NFR-12 | Scalability    | Consumer concurrency **should** be set to match the number of partitions (default: 3) to enable maximum parallel throughput. |
| NFR-13 | Scalability    | Topic replicas **should** be increased to 3 in production to tolerate broker failures without data loss. |
| NFR-14 | Configurability| Kafka bootstrap servers **must** be externally configurable via the KAFKA_BOOTSTRAP_SERVERS environment variable. |
