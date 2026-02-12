package com.aws.carddemo.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aws.carddemo.exception.DuplicateKeyException;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.user.dto.UserCreateRequest;
import com.aws.carddemo.user.dto.UserResponse;
import com.aws.carddemo.user.dto.UserUpdateRequest;

@Service
@Transactional
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, Pageable pageable) {
        Page<AppUser> users;
        if (search != null && !search.isBlank()) {
            users = appUserRepository.findByUserIdContainingIgnoreCase(search, pageable);
        } else {
            users = appUserRepository.findAll(pageable);
        }
        return users.map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.from(user);
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (appUserRepository.existsByUserId(request.userId())) {
            throw new DuplicateKeyException("User already exists with userId: " + request.userId());
        }

        AppUser user = new AppUser();
        user.setUserId(request.userId());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setUserType(request.userType());

        AppUser saved = appUserRepository.save(user);
        return UserResponse.from(saved);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.userType() != null) {
            user.setUserType(request.userType());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        AppUser saved = appUserRepository.save(user);
        return UserResponse.from(saved);
    }

    public void deleteUser(Long id) {
        if (!appUserRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        appUserRepository.deleteById(id);
    }
}
