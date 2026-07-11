package org.intern.shopeefoodclone.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.auth.blacklist.TokenBlacklistService;
import org.intern.shopeefoodclone.auth.otp.OtpRequest;
import org.intern.shopeefoodclone.auth.otp.UserOtpService;
import org.intern.shopeefoodclone.config.security.JwtService;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.intern.shopeefoodclone.infras.messaging.KafkaEventPublisher;
import org.intern.shopeefoodclone.infras.notification.EmailService;
import org.intern.shopeefoodclone.shared.constant.DATE;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.intern.shopeefoodclone.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
class AuthService {

    UserService userService;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    HttpServletRequest currentRequest;
    HttpServletResponse currentResponse;
    UserOtpService userOtpService;
    TokenBlacklistService tokenBlacklistService;
    KafkaEventPublisher kafkaEventPublisher;

    public AuthResponse login(LoginRequest loginRequest) {

        String userEmail = loginRequest.email();
        String userPassword = loginRequest.password();

        User user = userService.findByEmail(userEmail);

        if (!passwordEncoder.matches(userPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Password is incorrect");
        }

        if (user.getVerifiedAt() == null) {
            throw new AppException(ErrorCode.USER_NOT_VERIFIED, "User is not verified. Please verify your email first.");
        }

        AuthResponse res  = generateTokensForUser(user.getId().toString());
        setRefreshTokenCookie(res.refreshToken());
        return res;
    }

    public String sendRegistrationOtp(OtpRequest req) {
        return userOtpService.generateAndSendRegistrationOtp(req.email());
    }

    @Transactional
    public UserResponse register(UserCreateRequest registerRequest) {
        UserResponse userResponse = userService.create(registerRequest);

        User newUser = userService.findByEmail(registerRequest.email());
        kafkaEventPublisher.publishUserRegistered(newUser);

        userOtpService.generateAndSendRegistrationOtp(registerRequest.email());

        return userResponse;
    }

    public AuthResponse refreshToken(String refreshToken) {
        jwtService.validateToken(refreshToken, false);
        String userId = jwtService.extractUserId(refreshToken);
        return generateTokensForUser(userId);

    }

    private void invalidateToken(String token) {
        long ttl = jwtService.extractTokenExpiration(token).getTime() - System.currentTimeMillis();

        if (ttl <= 0) {
            log.info("Token already expired, no need to blacklist");
            return;
        }

        String jid = jwtService.extractTokenId(token);
        String userId = jwtService.extractUserId(token);
        OffsetDateTime expiresAt = jwtService.extractTokenExpiration(token).toInstant().atOffset(ZoneOffset.UTC);
        log.info("Blacklisting token with JID {} expiring at {}", jid, expiresAt);
        tokenBlacklistService.revokeToken(jid, userId, expiresAt, "LOGOUT");
    }


    public void logout(String refreshToken) {

        invalidateToken(refreshToken);

        String header = currentRequest.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new AppException(ErrorCode.INVALID_TOKEN, "Token not found!");

        String accessToken = header.substring(7);

        invalidateToken(accessToken);

        clearRefreshTokenCookie();

    }

    private AuthResponse generateTokensForUser(String userId) {
        String accessToken = jwtService.generateToken(userId, true);
        String refreshToken = jwtService.generateToken(userId,  false);

        return new AuthResponse(accessToken, refreshToken);
    }


    private void setRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Ensure HTTPS in prod
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        currentResponse.addCookie(cookie);
    }

    private void clearRefreshTokenCookie() {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        currentResponse.addCookie(cookie);
    }


    public AuthResponse verifyOtp(OtpRequest request) {
        String email = request.email();
        String otp = request.otp();


        if (otp == null || otp.isBlank()) {
            throw new AppException(ErrorCode.INVALID_OTP, "OTP is missing");
        }

        User user = userService.findByEmail(email); // Check if user exists
        userOtpService.verifyRegistrationOtp(email, otp);

        user.setVerifiedAt(DATE.now());
        userService.update(user.getId(), UserUpdateRequest.builder()
                    .verifiedAt(user.getVerifiedAt()).build());

        return generateTokensForUser(user.getId().toString());
    }
}
