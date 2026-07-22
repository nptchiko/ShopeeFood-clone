package org.intern.shopeefoodclone.auth.otp;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.infras.messaging.KafkaEventPublisher;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.user.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UserOtpService {

    UserOtpRepository userOtpRepository;
    CacheService cacheService;
    KafkaEventPublisher kafkaEventPublisher;
    UserService userService;
    SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long OTP_TTL_SECONDS = 300L; // 5 minutes

    @Transactional
    public String generateAndSendRegistrationOtp(String email) {

        int randomNum = 100000 + SECURE_RANDOM.nextInt(900000);
        String otp = String.valueOf(randomNum);
        String redisKey = "otp:register:" + email;

        // 1. Save to Redis cache via CacheService
        cacheService.saveOtp(redisKey, otp, OTP_TTL_SECONDS);

        // 2. Save to DB storage
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(OTP_TTL_SECONDS);

        UserOtp userOtp = userOtpRepository.findByEmail(email).orElse(null);
        if (userOtp != null) {
            userOtp.setOtp(otp);
            userOtp.setExpiresAt(expiresAt);
        } else {
            userOtp = UserOtp.builder()
                    .email(email)
                    .otp(otp)
                    .expiresAt(expiresAt)
                    .build();
        }
        userOtpRepository.save(userOtp);

        log.info("Generated registration OTP {} for email {}", otp, email);
        // Publish event to Kafka — the KafkaNotificationConsumer will handle email delivery.
        // This decouples OTP generation from SMTP and provides durability: if email
        // delivery fails, Kafka retries up to 3 times before routing to the DLT.
        kafkaEventPublisher.publishOtpVerificationRequested(email, otp);
        return otp;
    }

    @Transactional
    public void verifyRegistrationOtp(String email, String otp) {

        String redisKey = "otp:register:" + email;

        // Step 1: Redis check
        String cachedOtp = cacheService.getOtp(redisKey);
        if (cachedOtp != null) {
            log.debug("Found OTP in Redis cache for email: {}", email);
            if (!cachedOtp.equals(otp)) {
                throw new AppException(ErrorCode.INVALID_OTP, "The OTP code is incorrect.");
            }
            // Valid OTP in Redis -> cleanup both storages
            cacheService.deleteOtp(redisKey);
            userOtpRepository.deleteByEmail(email);
            return;
        }

        // Step 2: If failed (cache miss), search in DB storage
        log.info("OTP not found in Redis cache for email {}, checking database storage...", email);
        UserOtp dbOtp = userOtpRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, "The OTP code was not found or has expired. Please request a new one."));

        if (dbOtp.getExpiresAt().isBefore(OffsetDateTime.now())) {
            userOtpRepository.delete(dbOtp);
            throw new AppException(ErrorCode.VERIFICATION_TOKEN_NOT_FOUND, "The OTP code has expired. Please request a new one.");
        }

        if (!dbOtp.getOtp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_OTP, "The OTP code is incorrect.");
        }

        // Valid OTP in DB -> cleanup
        userOtpRepository.delete(dbOtp);
    }

    // Cleanup mechanism for DB storage (runs every hour)
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Running scheduled cleanup of expired OTPs from database storage...");
        userOtpRepository.deleteByExpiresAtBefore(OffsetDateTime.now());
        log.info("Completed cleanup of expired OTPs.");
    }
}
