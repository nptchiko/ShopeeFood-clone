package org.intern.shopeefoodclone.user;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.auth.RegisterRequest;
import org.intern.shopeefoodclone.shared.constant.PredefinedRole;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class UserService {

    UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsManager userDetailsManager;

    @Transactional
    public UserResponse create(RegisterRequest registerRequest) {

       if (userRepository.existsByEmail(registerRequest.email()))
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "User already exists with email: " + registerRequest.email());

       User newUser = userMapper.toEntity(registerRequest);
       newUser.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
       newUser.setRole(PredefinedRole.USER.name());

        return userMapper.toResponse(userRepository.save(newUser));
    }


    public List<User> findAll() {
        return userRepository.findAll();
    }


    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with email: " + email));
    }

    @Transactional
    public User update(UUID id, User updated) {
        return userRepository.findById(id).map(user -> {
            userMapper.update(user, updated);
            user.setPasswordHash(passwordEncoder.encode(updated.getPasswordHash()));
            return userRepository.save(user);
        }).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found with id: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        User existing = findById(id);
        userRepository.delete(existing);
    }
}
