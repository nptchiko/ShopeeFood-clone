package org.intern.shopeefoodclone.auth.blacklist;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class TokenBlacklistService {

    RevokedTokenRepository repository;

    @Transactional
    public void revokeToken(String jti, String userIdStr, OffsetDateTime expiresAt, String reason) {
        if (jti == null || jti.isBlank()) {
            return;
        }

        if (expiresAt.isAfter(OffsetDateTime.now())) {
            if (!repository.existsByJti(jti)) {
                UUID userId = UUID.fromString(userIdStr);
                RevokedToken revokedToken = RevokedToken.builder()
                        .jti(jti)
                        .userId(userId)
                        .expiresAt(expiresAt)
                        .reason(reason)
                        .build();
                repository.save(revokedToken);
                log.info("Revoked JWT [jti={}] for user [userId={}] with reason: {}", jti, userId, reason);
            } else {
                log.info("JWT [jti={}] is already revoked in database storage", jti);
            }
        } else {
            log.info("JWT [jti={}] already expired, skipping blacklist insertion", jti);
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return repository.existsByJti(jti);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Running scheduled cleanup of expired JWTs from database storage...");
        repository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.info("Completed cleanup of expired JWTs.");
    }
}
