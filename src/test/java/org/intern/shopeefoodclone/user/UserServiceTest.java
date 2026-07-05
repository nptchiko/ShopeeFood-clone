package org.intern.shopeefoodclone.user;

import org.intern.shopeefoodclone.shared.constant.PredefinedRole;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class
UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .phone("1234567890")
                .passwordHash("hashed_password")
                .role(PredefinedRole.USER.name())
                .build();

        userResponse = new UserResponse(
                userId,
                "Test User",
                "test@example.com",
                "1234567890",
                PredefinedRole.USER.name()
        );
    }

    @Test
    void testCreateUser_Success() {
        UserCreateRequest request = new UserCreateRequest(
                "Test User",
                "test@example.com",
                "1234567890",
                "Password123!");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.create(request);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Test User", result.name());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_AlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(
                "Test User",
                "test@example.com",
                "1234567890",
                "Password123!"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.create(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFindAll_Success() {
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponseList(List.of(user))).thenReturn(List.of(userResponse));

        List<UserResponse> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals(userResponse, result.get(0));
        verify(userRepository).findAll();
    }

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        verify(userRepository).findById(userId);
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.getUserById(userId));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testUpdateUser_Success() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phone("0987654321")
                .password("NewPassword123!")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("new_encoded_password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(
                userId,
                "Updated Name",
                "updated@example.com",
                "0987654321",
                "USER"
        ));

        UserResponse result = userService.update(userId, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.name());
        verify(userMapper).update(user, request);
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUser_EmailAlreadyExists() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("existing@example.com")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.update(userId, request));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        UserUpdateRequest request = UserUpdateRequest.builder().name("Name").build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.update(userId, request));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void testDeleteUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.delete(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.delete(userId));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).delete(any(User.class));
    }
}
