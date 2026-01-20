package com.carddemo.admin.service;

import com.carddemo.admin.dto.AdminUserDto;
import com.carddemo.admin.dto.CreateUserRequest;
import com.carddemo.admin.dto.UpdateUserRequest;
import com.carddemo.admin.entity.AdminUser;
import com.carddemo.admin.repository.AdminUserRepository;
import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminUserDto getUserById(String userId) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        return mapToUserDto(user);
    }

    public PageResponse<AdminUserDto> getAllUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<AdminUser> userPage = userRepository.findAll(pageable);
        return buildPageResponse(userPage);
    }

    public PageResponse<AdminUserDto> getUsersByType(String userType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").ascending());
        Page<AdminUser> userPage = userRepository.findByUserType(userType, pageable);
        return buildPageResponse(userPage);
    }

    public PageResponse<AdminUserDto> searchUsersByLastName(String lastName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
        Page<AdminUser> userPage = userRepository.findByLastNameContainingIgnoreCase(lastName, pageable);
        return buildPageResponse(userPage);
    }

    @Transactional
    public AdminUserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new BusinessException("User ID already exists: " + request.getUserId(), "USER_EXISTS");
        }

        AdminUser user = AdminUser.builder()
                .userId(request.getUserId().toUpperCase())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .active(true)
                .build();

        AdminUser savedUser = userRepository.save(user);
        return mapToUserDto(savedUser);
    }

    @Transactional
    public AdminUserDto updateUser(String userId, UpdateUserRequest request) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        AdminUser savedUser = userRepository.save(user);
        return mapToUserDto(savedUser);
    }

    @Transactional
    public void deleteUser(String userId) {
        AdminUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void hardDeleteUser(String userId) {
        if (!userRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.deleteById(userId);
    }

    private PageResponse<AdminUserDto> buildPageResponse(Page<AdminUser> userPage) {
        List<AdminUserDto> users = userPage.getContent().stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());

        return PageResponse.<AdminUserDto>builder()
                .content(users)
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    private AdminUserDto mapToUserDto(AdminUser user) {
        String typeDescription = "A".equals(user.getUserType()) ? "Administrator" : "Regular User";

        return AdminUserDto.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userType(user.getUserType())
                .userTypeDescription(typeDescription)
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
