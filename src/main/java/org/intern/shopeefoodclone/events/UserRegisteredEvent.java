package org.intern.shopeefoodclone.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain event published when a new user successfully completes registration.
 *
 * Consumers of this event can independently react without coupling to the auth domain:
 *   - Notification service  → send welcome email
 *   - Analytics service     → track new user signups
 *   - Loyalty service       → provision welcome credits
 *
 * Schema version: 1
 * Topic: user.registered
 */
@Value
@Builder
public class UserRegisteredEvent {

    /**
     * Unique event identifier for idempotency checking on the consumer side.
     */
    UUID correlationId;

    /** The newly created user's unique ID. */
    UUID userId;

    /** The registered email address (used for welcome email delivery). */
    String email;

    /** The user's display name for personalized greeting. */
    String displayName;

    /** Timestamp when the user account was created (UTC). */
    OffsetDateTime registeredAt;

    @JsonCreator
    public UserRegisteredEvent(
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("userId") UUID userId,
            @JsonProperty("email") String email,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("registeredAt") OffsetDateTime registeredAt
    ) {
        this.correlationId = correlationId;
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.registeredAt = registeredAt;
    }
}
