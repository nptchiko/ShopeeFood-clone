package org.intern.shopeefoodclone.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.config.security.JwtService;
import org.intern.shopeefoodclone.config.security.RedisService;
import org.intern.shopeefoodclone.shared.constant.PredefinedRole;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.SecurityUtils;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserMapper;
import org.intern.shopeefoodclone.user.UserResponse;
import org.intern.shopeefoodclone.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
class AuthService {

    RedisService redisService;
    UserService userService;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    HttpServletRequest currentRequest;
    HttpServletResponse currentResponse;
    private final UserMapper userMapper;

    public AuthResponse login(LoginRequest loginRequest) {

        String userEmail = loginRequest.email();
        String userPassword = loginRequest.password();

        User user = userService.findByEmail(userEmail);

        if (!passwordEncoder.matches(userPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Password is incorrect");
        }

        AuthResponse res  = generateTokensForUser(user.getId().toString());
        setRefreshTokenCookie(res.refreshToken());
        return res;
    }

    public UserResponse register(RegisterRequest req) {
        User user = User.builder()
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(PredefinedRole.USER.name())
                .build();
        User savedUser = userService.create(user);
        return userMapper.toResponse(savedUser);
    }

    public AuthResponse refreshToken(String refreshToken) {

        jwtService.validateToken(refreshToken, false);
        String userId = SecurityUtils.getCurrentUserId();
        return generateTokensForUser(userId);

    }

    private void invalidateToken(String token) {
        long ttl = jwtService.extractTokenExpiration(token).getTime() - System.currentTimeMillis();

        if (ttl <= 0) {
            log.info("Token already expired, no need to blacklist");
            return;
        }

        String jid = jwtService.extractTokenId(token);
        log.info("Blacklisting token with JID {} for {} ms", jid, ttl);
        redisService.blacklistToken(jid, ttl);
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


}
