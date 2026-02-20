package com.carddemo.service;

import com.carddemo.dto.UserCreateRequest;
import com.carddemo.entity.UserSecurity;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.UserSecurityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {

    private final UserSecurityRepository userSecurityRepository;

    public UserAdminService(UserSecurityRepository userSecurityRepository) {
        this.userSecurityRepository = userSecurityRepository;
    }

    public Page<UserSecurity> listUsers(Pageable pageable) {
        return userSecurityRepository.findAll(pageable);
    }

    public UserSecurity getUser(String userId) {
        return userSecurityRepository.findById(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public UserSecurity createUser(UserCreateRequest request) {
        String userId = request.getUserId().toUpperCase();
        if (userSecurityRepository.existsById(userId)) {
            throw new IllegalArgumentException("User already exists: " + userId);
        }

        UserSecurity user = new UserSecurity();
        user.setUsrId(userId);
        user.setUsrFname(request.getFirstName());
        user.setUsrLname(request.getLastName());
        user.setUsrPwd(request.getPassword().toUpperCase());
        user.setUsrType(request.getUserType().toUpperCase());

        return userSecurityRepository.save(user);
    }

    @Transactional
    public UserSecurity updateUser(String userId, UserCreateRequest request) {
        UserSecurity existing = userSecurityRepository.findById(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getFirstName() != null) {
            existing.setUsrFname(request.getFirstName());
        }
        if (request.getLastName() != null) {
            existing.setUsrLname(request.getLastName());
        }
        if (request.getPassword() != null) {
            existing.setUsrPwd(request.getPassword().toUpperCase());
        }
        if (request.getUserType() != null) {
            existing.setUsrType(request.getUserType().toUpperCase());
        }

        return userSecurityRepository.save(existing);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (!userSecurityRepository.existsById(userId.toUpperCase())) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        userSecurityRepository.deleteById(userId.toUpperCase());
    }
}
