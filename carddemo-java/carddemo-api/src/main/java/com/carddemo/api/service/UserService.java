package com.carddemo.api.service;

import com.carddemo.api.dto.PageResponse;
import com.carddemo.api.dto.UserCreateRequest;
import com.carddemo.api.dto.UserResponse;
import com.carddemo.api.dto.UserUpdateRequest;
import com.carddemo.core.domain.UserSecurity;
import com.carddemo.core.exception.DuplicateResourceException;
import com.carddemo.core.exception.ResourceNotFoundException;
import com.carddemo.core.repository.UserSecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for User administration.
 * Replaces business logic from COUSR00C (List Users), COUSR01C (Add User),
 * COUSR02C (Update User), and COUSR03C (Delete User).
 *
 * Key COBOL logic replaced:
 * - VSAM READ/WRITE/REWRITE/DELETE on USRSEC file
 * - Password validation (COBOL stores plain text; Java uses BCrypt)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserSecurityRepository userSecurityRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        Page<UserSecurity> page = userSecurityRepository.findAll(pageable);
        return buildPageResponse(page);
    }

    public UserResponse getUser(String userId) {
        UserSecurity user = userSecurityRepository.findByUsrId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userSecurityRepository.existsByUsrId(request.getUserId())) {
            throw new DuplicateResourceException("User", request.getUserId());
        }

        UserSecurity user = UserSecurity.builder()
                .usrId(request.getUserId())
                .usrFirstName(request.getFirstName())
                .usrLastName(request.getLastName())
                .usrPassword(passwordEncoder.encode(request.getPassword()))
                .usrType(request.getUserType())
                .build();

        UserSecurity saved = userSecurityRepository.save(user);
        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        UserSecurity user = userSecurityRepository.findByUsrId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getFirstName() != null) {
            user.setUsrFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setUsrLastName(request.getLastName());
        }
        if (request.getPassword() != null) {
            user.setUsrPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getUserType() != null) {
            user.setUsrType(request.getUserType());
        }

        UserSecurity saved = userSecurityRepository.save(user);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteUser(String userId) {
        UserSecurity user = userSecurityRepository.findByUsrId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        userSecurityRepository.delete(user);
    }

    private UserResponse mapToResponse(UserSecurity user) {
        return UserResponse.builder()
                .userId(user.getUsrId())
                .firstName(user.getUsrFirstName())
                .lastName(user.getUsrLastName())
                .userType(user.getUsrType())
                .build();
    }

    private PageResponse<UserResponse> buildPageResponse(Page<UserSecurity> page) {
        return PageResponse.<UserResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
