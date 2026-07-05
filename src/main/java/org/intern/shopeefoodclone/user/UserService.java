package org.intern.shopeefoodclone.user;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.shared.constant.PredefinedRole;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "User already exists with email: " + request.email());
        }
        User newUser = userMapper.toEntity(request);
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        newUser.setRole(PredefinedRole.USER.name());
        return userMapper.toResponse(userRepository.save(newUser));
    }

    public List<UserResponse> findAll() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    public UserResponse getUserById(UUID id) {
        User user = findById(id);
        return userMapper.toResponse(user);
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with email: " + email));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = findById(id);

        if (request.email() != null && !request.email().equals(user.getEmail()) && userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        userMapper.update(user, request);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User existing = findById(id);
        userRepository.delete(existing);
    }
}
