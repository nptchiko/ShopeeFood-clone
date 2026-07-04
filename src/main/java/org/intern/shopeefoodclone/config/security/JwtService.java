package org.intern.shopeefoodclone.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.intern.shopeefoodclone.shared.constant.DATE;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.infras.cache.CacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Service
public class JwtService {

    private final CacheService cacheService;
    @Value("${jwt.secret}")
    String jwtSecret;

    public JwtService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    private SecretKey getSigningKey () {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Value("${jwt.expiration.access-token}")
    long accessExpiration;

    @Value("${jwt.expiration.refresh-token}")
    long refreshExpiration;

    static final String isAccessTokenClaim = "is_access_token";

    public String generateToken(String userId, boolean isAccessToken) {

        long expiration = isAccessToken ? accessExpiration : refreshExpiration;


        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(DATE.now().toInstant()))
                .expiration(Date.from(DATE.now().toInstant().plusSeconds(expiration)))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .claim(isAccessTokenClaim, isAccessToken)
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public void validateToken(String token, boolean isAccessToken) {
        if (token == null || token.trim().isEmpty() )
            throw new AppException(ErrorCode.INVALID_TOKEN, "Token not found!");

        try {

            if (cacheService.isTokenBlacklisted(extractTokenId(token)))
                throw new AppException(ErrorCode.INVALID_TOKEN, "Token has been revoked");

            Claims payload = getPayload(token);

            if (payload.getSubject().isBlank()) {
                throw new AppException(ErrorCode.INVALID_TOKEN, "Token subject is missing");
            }

            if (payload.get(isAccessTokenClaim) == null || (Boolean) payload.get(isAccessTokenClaim) != isAccessToken) {
                throw new AppException(ErrorCode.INVALID_TOKEN, "Token type mismatch");
            }

        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN, "Expired token");
        }
        catch (MalformedJwtException e) {
        log.error("Token parsing error: {}", e.getMessage());
        throw new AppException(ErrorCode.INVALID_TOKEN, "Token is malformed");
       }
        catch (JwtException e) {
            log.error("Token validation error: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN, "Token is invalid");
        } catch (NullPointerException e) {
            log.error("Token parsing error: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN, "Token credential is missing or malformed");
        }
        }

    public String extractUserId(String token){
        return getPayload(token).getSubject();
    }

    public Date extractTokenExpiration(String token) {
        return getPayload(token).getExpiration();
    }

    public String extractTokenId(String token) {
        String jid = getPayload(token).getId();
        if (jid == null) throw new AppException(ErrorCode.INVALID_TOKEN, "Token ID (JID) is missing");

        return jid;
    }

    private Claims getPayload(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
