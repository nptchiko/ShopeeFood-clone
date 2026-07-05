package org.intern.shopeefoodclone.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intern.shopeefoodclone.shared.constant.PredefinedRole;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.exception.GlobalExceptionalHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionalHandler())
                .build();

        userId = UUID.randomUUID();
        userResponse = new UserResponse(
                userId,
                "Test User",
                "test@example.com",
                "1234567890",
                PredefinedRole.USER.name()
        );
    }

    @Test
    void testCreateUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "Test User",
                "test@example.com",
                "1234567890",
                "Password123!",
                "USER"
        );

        when(userService.create(any(UserCreateRequest.class))).thenReturn(userResponse);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.name").value("Test User"));

        verify(userService).create(any(UserCreateRequest.class));
    }

    @Test
    void testCreateUser_ValidationFailure() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest(
                "",
                "invalid-email",
                "123",
                "short",
                "USER"
        );

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Request Data"))
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        when(userService.findAll()).thenReturn(List.of(userResponse));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(userId.toString()));

        verify(userService).findAll();
    }

    @Test
    void testGetUserById_Success() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(userId.toString()));

        verify(userService).getUserById(userId);
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        when(userService.getUserById(userId)).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(40016));

        verify(userService).getUserById(userId);
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .build();

        UserResponse updatedResponse = new UserResponse(
                userId,
                "Updated Name",
                "updated@example.com",
                "1234567890",
                PredefinedRole.USER.name()
        );

        when(userService.update(eq(userId), any(UserUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        verify(userService).update(eq(userId), any(UserUpdateRequest.class));
    }

    @Test
    void testUpdateUser_EmailAlreadyExists() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("existing@example.com")
                .build();

        when(userService.update(eq(userId), any(UserUpdateRequest.class)))
                .thenThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(40011));

        verify(userService).update(eq(userId), any(UserUpdateRequest.class));
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        doNothing().when(userService).delete(userId);

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).delete(userId);
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        doThrow(new AppException(ErrorCode.USER_NOT_FOUND)).when(userService).delete(userId);

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(40016));

        verify(userService).delete(userId);
    }
}
