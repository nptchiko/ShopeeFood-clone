# Authentication Feature — Software Requirements Specification

| Field | Value |
|---|---|
| **Project** | ShopeeFood Clone |
| **Module** | Authentication & Security |
| **File** | `auth-spec.md` |
| **Version** | 1.0.0 |
| **Date** | 2026-07-08 |
| **Status** | Draft |

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Overall Description](#2-overall-description)
3. [Data Models](#3-data-models)
4. [Functional Requirements](#4-functional-requirements)
5. [Flow Diagrams](#5-flow-diagrams)
6. [Business Rules](#6-business-rules)
7. [Error Catalogue](#7-error-catalogue)
8. [API Specification](#8-api-specification)
9. [Security Architecture](#9-security-architecture)
10. [Non-Functional Requirements](#10-non-functional-requirements)

---

## 1. Introduction

### 1.1 Purpose
This document defines the software requirements for the **Authentication** module of the ShopeeFood Clone backend. It specifies the registration, login, token lifecycle, OTP email verification, and logout flows, together with all associated security mechanisms.

### 1.2 Scope
The module covers:

| Sub-feature | Responsibility |
|---|---|
| **Registration** | User account creation + OTP email dispatch |
| **OTP Verification** | Confirm email ownership; mark account as verified; issue tokens |
| **Login** | Credential validation; JWT issuance |
| **Token Refresh** | Issue a new token pair from a valid refresh token |
| **Logout** | Dual-token revocation (access + refresh) via blacklist |
| **JWT Infrastructure** | Token generation, validation, claim extraction, blacklist check |
| **Token Blacklist** | Persisting revoked JTIs in PostgreSQL; hourly purge of expired entries |
| **OTP Store** | Redis-first with PostgreSQL fallback; 5-minute TTL; hourly purge |

### 1.3 Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.1.0 + Spring Security |
| Token format | JWT (HS256) via JJWT 0.12.5 |
| Cache layer | Redis (via CacheService) |
| Persistence | PostgreSQL (Flyway migrations) |
| Email | SMTP (EmailService) |
| Password hashing | BCrypt (PasswordEncoder) |
| Token ID | UUID v4 per token (jti claim) |

---

## 2. Overall Description

### 2.1 Authentication Flow (High Level)

```
Register → [OTP sent to email] → Verify OTP → [Tokens issued] → Authenticated

Login (verified user) → [Tokens issued] → Authenticated

Authenticated → Refresh → [New tokens issued]
Authenticated → Logout  → [Both tokens blacklisted]
```

### 2.2 Token Strategy

| Token | Location | TTL | Configurable |
|---|---|---|---|
| Access token | Authorization: Bearer header | Short (e.g. 15 min) | jwt.expiration.access-token |
| Refresh token | HttpOnly Secure cookie (refresh_token) + response body | Long (e.g. 7 days) | jwt.expiration.refresh-token |

Both tokens are JWTs signed with **HS256** using a shared secret (`jwt.secret`). Each token carries a unique `jti` UUID claim for revocation.

### 2.3 Public Endpoints (No Auth Required)

| Path |
|---|
| POST /api/auth/register |
| POST /api/auth/login |
| POST /api/auth/refresh |
| POST /api/auth/otp/send |
| POST /api/auth/otp/verify |
| GET /swagger-ui/** |
| GET /v3/api-docs/** |

All other endpoints require a valid `Authorization: Bearer <access_token>` header.

---

## 3. Data Models

### 3.1 user_otps Table

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK, auto | |
| email | VARCHAR | NOT NULL, UNIQUE | One pending OTP per email |
| otp | VARCHAR(6) | NOT NULL | 6-digit numeric code |
| expires_at | TIMESTAMPTZ | NOT NULL | now() + 5 min |
| created_at | TIMESTAMPTZ | NOT NULL, immutable | |

### 3.2 revoked_tokens Table

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK, auto | |
| jti | VARCHAR(36) | NOT NULL, UNIQUE | JWT token ID |
| user_id | UUID | NOT NULL, FK users | |
| revoked_at | TIMESTAMPTZ | NOT NULL, immutable | |
| expires_at | TIMESTAMPTZ | NOT NULL | Token original expiry |
| reason | VARCHAR(50) | nullable | e.g. LOGOUT |

> [!NOTE]
> revoked_tokens has indexes on expires_at (for purge queries) and user_id.

### 3.3 JWT Claims

| Claim | Key | Value |
|---|---|---|
| Subject | sub | User UUID string |
| Issued at | iat | Unix timestamp |
| Expiration | exp | Unix timestamp |
| Token ID | jti | UUID v4 string |
| Token type | is_access_token | true (access) / false (refresh) |

---

## 4. Functional Requirements

### 4.1 Registration

| ID | Requirement |
|---|---|
| A-01 | The system **shall** create a new user account from name, email, phone, and password. |
| A-02 | The system **shall** reject registration if a user with the same email already exists (40010). |
| A-03 | The system **shall** hash the password with BCrypt before persisting. |
| A-04 | On successful account creation, the system **shall** generate a 6-digit OTP and send it to the supplied email. |
| A-05 | The OTP **shall** be stored in Redis (primary) and PostgreSQL (fallback) with a 5-minute TTL. |
| A-06 | The newly registered account **shall** have verifiedAt = null until OTP verification succeeds. |

### 4.2 OTP — Send

| ID | Requirement |
|---|---|
| A-07 | The system **shall** expose a standalone resend-OTP endpoint that regenerates and re-sends the OTP for a given email. |
| A-08 | If an OTP record already exists for the email, it **shall** be overwritten (upsert), not duplicated. |
| A-09 | The new OTP **shall** replace the existing Redis entry with a fresh 5-minute TTL. |

### 4.3 OTP — Verify

| ID | Requirement |
|---|---|
| A-10 | The system **shall** verify the OTP against Redis first; on cache miss it **shall** fall back to PostgreSQL. |
| A-11 | If the OTP matches, the system **shall** mark the user account as verified (verifiedAt = now()). |
| A-12 | After successful verification, the system **shall** delete the OTP from both Redis and PostgreSQL. |
| A-13 | If the OTP has expired in the database, the system **shall** delete the record and return 40018 INVALID_OTP. |
| A-14 | On successful OTP verification, the system **shall** issue an access token and a refresh token. |

### 4.4 Login

| ID | Requirement |
|---|---|
| A-15 | The system **shall** authenticate a user by email and password. |
| A-16 | The system **shall** reject login if the password does not match the BCrypt hash (40001). |
| A-17 | The system **shall** reject login for unverified accounts (40301 USER_NOT_VERIFIED). |
| A-18 | On successful login, the system **shall** issue an access token and a refresh token. |
| A-19 | The refresh token **shall** additionally be set as an HttpOnly, Secure, path-scoped cookie (/api/auth/refresh). |

### 4.5 Token Refresh

| ID | Requirement |
|---|---|
| A-20 | The system **shall** accept a refresh token and issue a new access token + refresh token pair. |
| A-21 | The system **shall** validate that the token is a refresh token (is_access_token == false). |
| A-22 | The system **shall** reject revoked, expired, or malformed refresh tokens (40101). |

### 4.6 Logout

| ID | Requirement |
|---|---|
| A-23 | The system **shall** require a valid Authorization: Bearer access token header on logout. |
| A-24 | The system **shall** revoke **both** the access token and the refresh token by inserting their jti into revoked_tokens. |
| A-25 | Tokens that are already expired at logout time **shall** be silently skipped (no DB insert). |
| A-26 | The system **shall** clear the refresh_token cookie by setting Max-Age: 0. |

### 4.7 Token Validation (JwtAuthFilter)

| ID | Requirement |
|---|---|
| A-27 | Every protected request **shall** be intercepted and the Bearer token extracted from the Authorization header. |
| A-28 | The system **shall** reject tokens whose JTI is found in revoked_tokens. |
| A-29 | The system **shall** reject tokens with a type mismatch (refresh token used as access token). |
| A-30 | The system **shall** reject expired, malformed, or unsigned tokens. |
| A-31 | On successful validation, the authenticated user ID **shall** be stored in the SecurityContext. |

### 4.8 Scheduled Maintenance

| ID | Requirement |
|---|---|
| A-32 | The system **shall** purge expired OTP records from PostgreSQL every hour. |
| A-33 | The system **shall** purge expired revoked token records from PostgreSQL every hour. |

---

## 5. Flow Diagrams

### 5.1 Registration + Verification Flow

```
Client                     Server                   Email
  |                           |                        |
  |-- POST /register -------->|                        |
  |   { name, email, pw }     |-- create user          |
  |                           |-- generate OTP         |
  |                           |-- save OTP (Redis+DB)  |
  |                           |-- sendOtpEmail() ----->|
  |<-- 201 { user } ---------.|                        |
  |                           |             [OTP code in email]
  |                           |                        |
  |-- POST /otp/verify ------>|                        |
  |   { email, otp }          |-- Redis lookup         |
  |                           |-- match -> delete OTP  |
  |                           |-- set verifiedAt=now() |
  |                           |-- issue tokens         |
  |<-- 200 { tokens } -------|                        |
```

### 5.2 Login Flow

```
Client                     Server
  |                           |
  |-- POST /login ----------->|
  |   { email, password }     |-- find user by email
  |                           |-- BCrypt.matches(pw, hash)?
  |                           |-- verifiedAt != null?
  |                           |-- generateTokens()
  |                           |-- setRefreshTokenCookie()
  |<-- 200 { tokens } -------|
  |   Set-Cookie: refresh_token=... HttpOnly; Secure
```

### 5.3 Logout Flow

```
Client                     Server
  |                           |
  |-- POST /logout ---------->|
  |   Authorization: Bearer   |
  |   { refresh_token }       |
  |                           |-- extractJTI(refresh) -> revokeToken()
  |                           |-- extractJTI(access)  -> revokeToken()
  |                           |-- clearRefreshTokenCookie()
  |<-- 200 "Logged out" -----|
  |   Set-Cookie: refresh_token=; Max-Age=0
```

### 5.4 OTP Verification Decision Tree

```
verifyOtp(email, otp)
  |
  +-- Redis.get("otp:register:{email}")
  |     +-- HIT  -> compare OTP
  |     |          +-- match    -> delete Redis + delete DB -> mark verified -> issue tokens
  |     |          +-- no match -> throw INVALID_OTP (40018)
  |     |
  |     +-- MISS -> DB lookup by email
  |                 +-- not found -> throw VERIFICATION_TOKEN_NOT_FOUND (40404)
  |                 +-- expired   -> delete record -> throw VERIFICATION_TOKEN_NOT_FOUND (40404)
  |                 +-- found & valid
  |                       +-- compare OTP
  |                             +-- match    -> delete DB -> mark verified -> issue tokens
  |                             +-- no match -> throw INVALID_OTP (40018)
```

---

## 6. Business Rules

| ID | Rule |
|---|---|
| BR-01 | Email addresses are case-sensitive and must be unique across all users. |
| BR-02 | Passwords must be 8-20 characters and contain at least one digit, one uppercase, one lowercase, and one special character (@#$%^&+=!). |
| BR-03 | A user cannot log in until their email has been verified via OTP. |
| BR-04 | Only one OTP can be pending per email at a time; requesting a new one overwrites the previous. |
| BR-05 | OTPs expire after 5 minutes. |
| BR-06 | Both the access token AND the refresh token are revoked on logout. |
| BR-07 | A revoked token can never be re-activated, even if the original TTL has not elapsed. |
| BR-08 | Expired tokens are never inserted into the blacklist (no-op at logout time). |
| BR-09 | Each token carries a unique jti UUID; the blacklist is keyed on this JTI, not the raw token string. |
| BR-10 | The refresh token is always delivered via HttpOnly + Secure cookie in addition to the response body. |

---

## 7. Error Catalogue

| Error Code | HTTP | Constant | Trigger |
|---|---|---|---|
| 40000 | 400 | INVALID_INPUT | Bean validation failure on request body |
| 40001 | 400 | INVALID_CREDENTIALS | Password does not match stored hash |
| 40010 | 400 | USER_ALREADY_EXISTS | Registration with an already-taken email |
| 40016 | 400 | USER_NOT_FOUND | OTP verify / login for non-existent email |
| 40018 | 400 | INVALID_OTP | OTP is wrong or has expired |
| 40100 | 401 | UNAUTHENTICATED | No authentication provided |
| 40101 | 401 | INVALID_TOKEN | Token is missing, malformed, expired, revoked, or wrong type |
| 40102 | 401 | EXPIRED_TOKEN | Token TTL has elapsed |
| 40301 | 403 | USER_NOT_VERIFIED | Login attempted on unverified account |
| 40404 | 404 | VERIFICATION_TOKEN_NOT_FOUND | OTP record missing or deleted (cache + DB miss) |

---

## 8. API Specification

### Conventions
- **Base URL:** `http://localhost:8080`
- **Content-Type:** `application/json`
- **Auth:** None required for all endpoints in this module

---

#### POST /api/auth/register — Register a New User

**Request Body**

```json
{
  "name": "string (required, max 50)",
  "email": "string (required, valid email, max 50)",
  "phone": "string (optional, max 15, pattern: +XX XXXXXXXXXX)",
  "password": "string (required, 8-20 chars, digit+upper+lower+special)"
}
```

**Validation Rules**

| Field | Rule |
|---|---|
| name | Not blank, max 50 chars |
| email | Valid email format, max 50, globally unique |
| phone | Optional; regex: ^(\+\d{1,3}[- ]?)?\d{10}$ |
| password | Regex: ^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{8,20}$ |

**Response 201 Created**

```json
{
  "status": 201,
  "message": "User registered successfully",
  "data": {
    "id": "uuid",
    "name": "Nguyen Van A",
    "email": "a@example.com",
    "phone": "+84912345678",
    "role": "USER",
    "createdAt": "2026-07-08T10:00:00+07:00"
  }
}
```

> [!NOTE]
> A 6-digit OTP is automatically sent to the email. The account is not usable until POST /api/auth/otp/verify succeeds.

**Error Responses**

| Condition | Code | HTTP |
|---|---|---|
| Email already registered | 40010 | 400 |
| Validation failure | 40000 | 400 |

---

#### POST /api/auth/otp/send — (Re)send OTP

Generates a new 6-digit OTP and emails it. Can be used to resend after expiry.

**Request Body**

```json
{
  "email": "string (required, valid email)"
}
```

**Response 200 OK**

```json
{
  "status": 200,
  "message": "OTP sent successfully"
}
```

---

#### POST /api/auth/otp/verify — Verify OTP and Obtain Tokens

**Request Body**

```json
{
  "email": "string (required, valid email)",
  "otp": "string (required, 6-digit)"
}
```

**Response 200 OK**

```json
{
  "status": 200,
  "message": "OTP verified successfully",
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "eyJ..."
  }
}
```

```
Set-Cookie: refresh_token=eyJ...; HttpOnly; Secure; Path=/api/auth/refresh; Max-Age=604800
```

**Error Responses**

| Condition | Code | HTTP |
|---|---|---|
| OTP is incorrect | 40018 | 400 |
| OTP expired or not found | 40404 | 404 |
| User not found | 40016 | 400 |

---

#### POST /api/auth/login — Login

**Request Body**

```json
{
  "email": "string (required, valid email)",
  "password": "string (required)"
}
```

**Response 200 OK**

```json
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "eyJ..."
  }
}
```

```
Set-Cookie: refresh_token=eyJ...; HttpOnly; Secure; Path=/api/auth/refresh; Max-Age=604800
```

**Error Responses**

| Condition | Code | HTTP |
|---|---|---|
| User not found | 40016 | 400 |
| Wrong password | 40001 | 400 |
| Account not verified | 40301 | 403 |

---

#### POST /api/auth/refresh — Refresh Tokens

**Request Body**

```json
{
  "refresh_token": "eyJ..."
}
```

**Response 200 OK**

```json
{
  "status": 200,
  "message": "Token refreshed successfully",
  "data": {
    "access_token": "eyJ...",
    "refresh_token": "eyJ..."
  }
}
```

**Error Responses**

| Condition | Code | HTTP |
|---|---|---|
| Token missing / malformed / expired | 40101 | 401 |
| Token is revoked | 40101 | 401 |
| Token is access type (not refresh) | 40101 | 401 |

---

#### POST /api/auth/logout — Logout

Revokes both the access token and the refresh token.

**Headers Required**

```
Authorization: Bearer <access_token>
```

**Request Body**

```json
{
  "refresh_token": "eyJ..."
}
```

**Response 200 OK**

```json
{
  "status": 200,
  "message": "Logged out successfully"
}
```

```
Set-Cookie: refresh_token=; HttpOnly; Secure; Path=/api/auth/refresh; Max-Age=0
```

**Error Responses**

| Condition | Code | HTTP |
|---|---|---|
| Authorization header missing | 40101 | 401 |
| Access token invalid or revoked | 40101 | 401 |

---

## 9. Security Architecture

### 9.1 JWT Filter Chain

```
Incoming Request
      |
      v
JwtAuthFilter (after BasicAuthenticationFilter)
      |
      +-- Extract Authorization: Bearer <token>
      |     +-- Missing or no Bearer prefix -> pass through (fails at authorization layer)
      |
      +-- JwtService.validateToken(token, isAccessToken=true)
      |     +-- Blank/null              -> INVALID_TOKEN (40101)
      |     +-- JTI in revoked_tokens  -> INVALID_TOKEN (40101)
      |     +-- Expired                -> INVALID_TOKEN (40101)
      |     +-- Malformed              -> INVALID_TOKEN (40101)
      |     +-- Wrong type flag        -> INVALID_TOKEN (40101)
      |
      +-- Set SecurityContext principal = userId (UUID string)
```

### 9.2 OTP Storage Strategy

```
Generate OTP
  +-- Save to Redis  key="otp:register:{email}"  TTL=300s
  +-- Upsert to DB   table=user_otps              expires_at=now()+5min

Verify OTP
  +-- Redis HIT  -> validate -> cleanup both -> proceed
  +-- Redis MISS -> DB lookup -> validate -> cleanup DB -> proceed

Scheduled (every hour)
  +-- DELETE FROM user_otps WHERE expires_at < now()
```

### 9.3 Token Blacklist Strategy

```
Logout / Revocation
  +-- For each token (access + refresh):
        +-- Extract: jti, userId, expiresAt
        +-- If already expired    -> skip (no DB insert)
        +-- If not yet expired    -> INSERT INTO revoked_tokens

JwtAuthFilter validation
  +-- SELECT EXISTS(... WHERE jti = ?)

Scheduled (every hour)
  +-- DELETE FROM revoked_tokens WHERE expires_at < now()
```

### 9.4 Password Security

- Hashed with **BCrypt** via Spring Security PasswordEncoder
- Raw passwords are never stored or logged
- Password change invalidates the stored hash only; existing tokens remain valid until expiry or explicit logout

---

## 10. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Security | All auth endpoints **must** be served over HTTPS in production. |
| NFR-02 | Security | Refresh token cookies **must** be HttpOnly and Secure to prevent XSS theft. |
| NFR-03 | Security | JWT secret **must** be at least 256 bits and loaded from environment variables — never hard-coded. |
| NFR-04 | Performance | OTP verification **should** check Redis first to avoid unnecessary DB round-trips. |
| NFR-05 | Availability | SMTP failures **should** be logged but **must not** fail the registration response. |
| NFR-06 | Scalability | Token blacklist and OTP tables are bounded by TTL — expired rows are purged hourly. |
| NFR-07 | Observability | All token revocations, OTP generations, and login failures **should** be logged at INFO or WARN level. |
| NFR-08 | Statelessness | The API is fully stateless — no server-side HTTP session; all auth state is in JWT + blacklist DB. |
| NFR-09 | Token TTL | Access token TTL **should** be short (<=15 min). Refresh token TTL **should** be 7 days. Both **must** be externally configurable. |
