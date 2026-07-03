package org.intern.shopeefoodclone.user;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.auth.RegisterRequest;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(RegisterRequest registerRequest) {

       if (!userRepository.existsByEmail(registerRequest.email()))
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

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User update(UUID id, User updated) {
        User existing = findById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        // For a real app, password should be hashed if changed, but we just set it here.
        existing.setPasswordHash(updated.getPasswordHash());
        existing.setRole(updated.getRole());
        return userRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        User existing = findById(id);
        userRepository.delete(existing);
    }
}
