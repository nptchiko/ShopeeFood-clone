package org.intern.shopeefoodclone.infras.cache;

public interface CacheService {
    void set(String key, String value, long durationSeconds);
    String get(String key);
    void delete(String key);
    boolean hasKey(String key);

    default void blacklistToken(String tokenId, long expirationDurationSeconds) {
        set("blacklisted:" + tokenId, "true", expirationDurationSeconds);
    }

    default boolean isTokenBlacklisted(String tokenId) {
        return hasKey("blacklisted:" + tokenId);
    }

    default void saveOtp(String key, String otp, long durationSeconds) {
        set(key, otp, durationSeconds);
    }

    default String getOtp(String key) {
        return get(key);
    }

    default void deleteOtp(String key) {
        delete(key);
    }
}
