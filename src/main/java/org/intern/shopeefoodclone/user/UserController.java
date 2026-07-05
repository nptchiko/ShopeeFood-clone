package org.intern.shopeefoodclone.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UserController {

    UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ApiResponse.created(response, "User created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(userService.getUserById(id), "User retrieved successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request), "User updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ApiResponse.success("User deleted successfully");
    }
}
