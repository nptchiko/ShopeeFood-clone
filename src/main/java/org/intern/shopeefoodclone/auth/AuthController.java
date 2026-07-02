package org.intern.shopeefoodclone.auth;

import jakarta.validation.Valid;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.user.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login successful")
                .data(response)
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse response = authService.register(registerRequest);
        return ApiResponse.<UserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("User registered successfully")
                .data(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest) {

        if (cookieToken == null && refreshTokenRequest == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token is missing");
        }
        String tokenToUse = cookieToken != null && !cookieToken.isBlank() ? cookieToken : refreshTokenRequest.refreshToken();

        AuthResponse response = authService.refreshToken(tokenToUse);

        return ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Token refreshed successfully")
                .data(response)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenRequest bodyToken) {

        if (cookieToken == null && bodyToken == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Refresh token is missing");
        }

        String cookieTokenToUse = cookieToken != null && !cookieToken.isBlank() ? cookieToken : bodyToken.refreshToken();
        authService.logout(cookieTokenToUse);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }
}
