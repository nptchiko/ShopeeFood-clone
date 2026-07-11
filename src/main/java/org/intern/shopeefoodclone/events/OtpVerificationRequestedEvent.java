package org.intern.shopeefoodclone.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event published when a user requests an OTP verification code.
 *
 * This event decouples the OTP generation logic (auth domain) from the
 * notification delivery logic (infra/notification domain).
 *
 * Schema version: 1
 * Topic: otp.verification.requested
 * Retention: default (7 days) — sufficient for retry/replay
 */
@Value
@Builder
public class OtpVerificationRequestedEvent {

    /**
     * Unique event identifier for idempotency checking on the consumer side.
     * If the same correlationId is seen twice, the consumer can safely skip it.
     */
    UUID correlationId;

    /** Target email address to deliver the OTP to. */
    String email;

    /** The 6-digit OTP code to include in the verification email. */
    String otpCode;

    /** Timestamp when this event was created (UTC). */
    OffsetDateTime requestedAt;

    @JsonCreator
    public OtpVerificationRequestedEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("email") String email,
            @JsonProperty("otpCode") String otpCode,
            @JsonProperty("requestedAt") OffsetDateTime requestedAt
    ) {
        this.correlationId = correlationId;
        this.email = email;
        this.otpCode = otpCode;
        this.requestedAt = requestedAt;
    }
}
