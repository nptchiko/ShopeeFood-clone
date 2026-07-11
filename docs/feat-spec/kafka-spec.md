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

| Sub-feature              | Responsibility                                               |
|---|---|
| **Event Publishing**      | Produce domain events to Kafka topics after state changes   |
| **Notification Consumer** | Consume events and dispatch email notifications via SMTP    |
| **OTP Delivery**          | Async dispatch of 6-digit OTP emails                        |
| **Welcome Email Delivery**| Async dispatch of greeting + OTP email on registration      |
| **Reliability**           | At-least-once delivery via manual ACK + retry + DLT         |
| **Dead-Letter Handling**  | Route poison-pill messages to DLT after max retries         |
| **Observability**         | Correlation ID propagation across all event boundaries       |

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

```
Auth Domain                  Kafka Broker              Notification Domain
    |                            |                            |
    |-- publishUserRegistered --> [user.registered] --------> handleUserRegistered()
    |                            |                            |-- sendEmail() (greeting + OTP)
    |                            |                            |
    |-- publishOtpVerification-> [otp.verification.requested] -> handleOtpVerificationRequested()
    |                            |                            |-- sendOtpEmail()
    |                            |                            |
    |                     [*.DLT] (on failure)               |
```

### 2.2 Key Design Decisions

| Decision                    | Rationale                                                                          |
|---|---|
| **At-least-once delivery**  | Offset committed via manual ACK only on success — never lost even on crash          |
| **Email as partition key**  | All events for the same user land on the same partition; ordering guaranteed        |
| **Type headers enabled**    | spring.json.use.type.headers: true lets the deserializer resolve the exact event class without a mapping registry |
| **Dead-Letter Topics**      | Failed messages after 3 retries are routed to *.DLT for inspection/replay          |
| **Correlation ID per event**| UUID generated at publish time, logged by both producer and consumer for end-to-end tracing |
| **Decoupled notification**  | Email delivery is entirely in KafkaNotificationConsumer — auth domain has zero SMTP dependency |

### 2.3 Consumer Group

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

Fired whenever the system generates a new OTP code — either on initial registration or on explicit OTP resend.

#### Schema

| Field           | Type            | Nullable | Description                                           |
|---|---|---|---|
| correlationId   | UUID            | No       | Unique event ID for idempotency and distributed tracing |
| email           | String          | No       | Recipient email address; also used as Kafka partition key |
| otpCode         | String (6-digit)| No       | The OTP code to embed in the verification email       |
| requestedAt     | OffsetDateTime  | No       | UTC timestamp when the event was created              |

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

Fired when a new user account is successfully created. Carries the OTP code so a single email can serve as both the welcome greeting and the verification prompt.

#### Schema

| Field           | Type            | Nullable | Description                                                      |
|---|---|---|---|
| correlationId   | UUID            | No       | Unique event ID for idempotency and distributed tracing          |
| userId          | UUID            | No       | The newly created user's primary key                             |
| email           | String          | No       | Registered email address; also used as Kafka partition key       |
| displayName     | String          | No       | User's display name for personalised email greeting              |
| otpCode         | String (6-digit)| Yes      | OTP code to include in welcome email; null if not applicable     |
| registeredAt    | OffsetDateTime  | No       | UTC timestamp when the user account was created                  |

#### Example Payload (JSON)

```json
{
  "correlationId": "f7e8d9c0-b1a2-3456-7890-abcdef123456",
  "userId": "d4e5f6a7-b8c9-0123-4567-890abcdef012",
  "email": "miku@example.com",
  "displayName": "Miku Nakano",
  "otpCode": "482931",
  "registeredAt": "2026-07-11T06:15:00Z"
}
```

---

## 4. Topic Configuration

### 4.1 Topics

| Topic Name                         | Partitions | Replicas           | Purpose                                              |
|---|---|---|---|
| otp.verification.requested         | 3          | 1 (dev) / 3 (prod) | OTP dispatch events                                  |
| user.registered                    | 3          | 1 (dev) / 3 (prod) | New user registration events                         |
| otp.verification.requested.DLT     | 1          | 1                  | Dead-letter for OTP events after max retries         |
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

### 5.1 Event Publishing

| ID    | Requirement |
|---|---|
| K-01  | The system **shall** publish an OtpVerificationRequestedEvent to otp.verification.requested each time an OTP is generated for a user email. |
| K-02  | The system **shall** publish a UserRegisteredEvent to user.registered upon successful user account creation, including the associated OTP code. |
| K-03  | Each published event **shall** include a unique correlationId (UUID v4) generated at publish time. |
| K-04  | The user's email address **shall** be used as the Kafka message partition key for all events. |
| K-05  | The producer **shall** embed a Java type header into every Kafka record so consumers can deserialize to the exact event class without a type mapping registry. |
| K-06  | On broker acknowledgment, the publisher **shall** log the correlationId, partition, and offset at DEBUG level. |
| K-07  | On broker send failure, the publisher **shall** log the correlationId, email, and error message at ERROR level without re-throwing. |

### 5.2 OTP Email Consumer

| ID    | Requirement |
|---|---|
| K-08  | The consumer **shall** listen on topic otp.verification.requested and call EmailService.sendOtpEmail(email, otpCode) upon receipt. |
| K-09  | The consumer **shall** commit the offset (via Acknowledgment.acknowledge()) **only** after the email service call returns successfully. |
| K-10  | If EmailService.sendOtpEmail() throws, the consumer **shall** re-throw the exception to allow the DefaultErrorHandler to apply the retry policy. |
| K-11  | The consumer **shall** log the correlationId, email, partition, and offset at INFO level upon receiving each event. |

### 5.3 Welcome + OTP Email Consumer

| ID    | Requirement |
|---|---|
| K-12  | The consumer **shall** listen on topic user.registered and dispatch a welcome email upon receipt. |
| K-13  | If the event carries a non-blank otpCode, the welcome email body **shall** include the verification code. |
| K-14  | If the event's otpCode is null or blank, the welcome email **shall** be sent without a verification code section. |
| K-15  | The consumer **shall** call EmailService.sendEmail(email, subject, body) with subject "Welcome to ShopeeFood!". |
| K-16  | The consumer **shall** commit the offset only after the email service call returns successfully. |
| K-17  | The consumer **shall** log the correlationId, userId, email, partition, and offset at INFO level upon receiving each event. |

### 5.4 Error Recovery

| ID    | Requirement |
|---|---|
| K-18  | The system **shall** retry failed consumer processing up to **3 times** with a **2-second fixed backoff** before routing to the DLT. |
| K-19  | Deserialization errors **shall** be caught by ErrorHandlingDeserializer and routed directly to the DLT without triggering the retry policy. |
| K-20  | Messages routed to a DLT **shall** retain the original topic name, partition, offset, and error details in the Kafka record headers. |

---

## 6. Flow Diagrams

### 6.1 Registration with Combined Welcome + OTP Email

```
Client            AuthService         UserOtpService      KafkaEventPublisher   KafkaNotificationConsumer  EmailService
  |                   |                    |                      |                       |                     |
  |- POST /register ->|                    |                      |                       |                     |
  |                   |- userService.create()                     |                       |                     |
  |                   |- generateAndSendOtp() ------------------>  |                       |                     |
  |                   |   (returns otp)    |- publishOtpVerification(email, otp) -------> |                     |
  |                   |                    |                      | [otp.verification.requested]                |
  |                   |                    |                      |                       |- sendOtpEmail() --> |
  |                   |- publishUserRegistered(user, otp) ------> |                       |                     |
  |                   |                    |                      | [user.registered]     |                     |
  |                   |                    |                      |                       |- sendEmail() -----> |
  |<-- 201 { user } --|                    |                      |                       |  (welcome + OTP)   |
```

### 6.2 OTP Resend Flow

```
Client            AuthService         UserOtpService      KafkaEventPublisher   KafkaNotificationConsumer  EmailService
  |                   |                    |                      |                       |                     |
  |- POST /otp/send ->|                    |                      |                       |                     |
  |                   |- generateAndSendOtp(email) -------------> |                       |                     |
  |                   |                    |- publishOtpVerification(email, newOtp) ----> |                     |
  |                   |                    |                      | [otp.verification.requested]                |
  |                   |                    |                      |                       |- sendOtpEmail() --> |
  |<-- 200 OK --------|                    |                      |                       |                     |
```

### 6.3 Error Handling & DLT Flow

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

```
[PUBLISH]   [Kafka] Publishing UserRegisteredEvent  | correlationId=abc-123 | email=miku@example.com
[BROKER]    [Kafka] UserRegisteredEvent sent         | correlationId=abc-123 | partition=1 | offset=42
[CONSUME]   [Kafka] Received UserRegisteredEvent     | correlationId=abc-123 | partition=1 | offset=42
[COMMIT]    [Kafka] UserRegisteredEvent processed    | correlationId=abc-123
```

---

## 7. Business Rules

| ID    | Rule |
|---|---|
| BR-01 | Each domain event is **immutable** once published. Consumers must never modify or re-publish a received event payload. |
| BR-02 | The correlationId is a UUID v4 generated at publish time and **must not** be reused across events. |
| BR-03 | Consumers **must** commit offsets only after successfully completing all side effects (e.g., email dispatch). |
| BR-04 | The email field **must** be used as the Kafka message key — never null or an empty string. |
| BR-05 | When UserRegisteredEvent.otpCode is non-null and non-blank, the welcome email **must** contain the OTP code. |
| BR-06 | A single registration triggers **two** events: OtpVerificationRequestedEvent (from UserOtpService) and UserRegisteredEvent (from AuthService). Consumers are independent and must not deduplicate across topics. |
| BR-07 | Messages routed to a DLT **must** be treated as an alert condition and investigated; no automatic re-processing is implemented. |
| BR-08 | The Kafka producer **must** be configured with enable.idempotence=true and acks=all to guarantee exactly-once writes to the broker. |

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
| KafkaEventPublisher             | infras.messaging  | Domain-facing publisher API; wraps KafkaTemplate   |
| KafkaNotificationConsumer       | infras.messaging  | Kafka listener for notification topics             |
| OtpVerificationRequestedEvent   | events            | Event POJO — OTP dispatch                          |
| UserRegisteredEvent             | events            | Event POJO — user registration + welcome email     |

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
| NFR-07 | Observability  | Every event **must** carry a UUID correlationId that is logged by both the publisher and the consumer, enabling end-to-end distributed tracing. |
| NFR-08 | Observability  | Producer send confirmation (partition + offset) **must** be logged at DEBUG level. Consumer receipt and commit **must** be logged at INFO level. |
| NFR-09 | Ordering       | All events for a given user email **must** be written to the same Kafka partition (email as message key) to preserve per-user ordering. |
| NFR-10 | Security       | The spring.json.trusted.packages setting **must** be scoped to org.intern.shopeefoodclone.events to prevent deserialization of arbitrary classes. |
| NFR-11 | Scalability    | Consumer concurrency **should** be set to match the number of partitions (default: 3) to enable maximum parallel throughput. |
| NFR-12 | Scalability    | Topic replicas **should** be increased to 3 in production to tolerate broker failures without data loss. |
| NFR-13 | Configurability| Kafka bootstrap servers **must** be externally configurable via the KAFKA_BOOTSTRAP_SERVERS environment variable. |
