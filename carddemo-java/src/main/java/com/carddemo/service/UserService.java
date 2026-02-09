package com.carddemo.service;

import com.carddemo.dto.UserDto;
import com.carddemo.entity.User;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User authenticate(String userId, String password) {
        String upperUserId = userId.toUpperCase().trim();
        String upperPassword = password.toUpperCase().trim();

        User user = userRepository.findById(upperUserId).orElse(null);
        if (user == null) {
            throw new ResourceNotFoundException("User not found. Try again ...");
        }
        if (!upperPassword.equals(user.getPassword())) {
            throw new ValidationException("Wrong Password. Try again ...");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public Page<User> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(String userId) {
        return userRepository.findById(userId.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    public User addUser(UserDto dto) {
        String upperUserId = dto.getUserId().toUpperCase().trim();
        if (userRepository.findById(upperUserId).isPresent()) {
            throw new ValidationException("User already exists: " + upperUserId);
        }

        User user = new User();
        user.setUserId(upperUserId);
        user.setFirstName(dto.getFirstName() != null ? dto.getFirstName().toUpperCase().trim() : "");
        user.setLastName(dto.getLastName() != null ? dto.getLastName().toUpperCase().trim() : "");
        user.setPassword(dto.getPassword() != null ? dto.getPassword().toUpperCase().trim() : upperUserId);
        user.setUserType(dto.getUserType() != null ? dto.getUserType().toUpperCase().trim() : "U");

        return userRepository.save(user);
    }

    public User updateUser(String userId, UserDto dto) {
        User existing = userRepository.findById(userId.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (dto.getFirstName() != null) {
            existing.setFirstName(dto.getFirstName().toUpperCase().trim());
        }
        if (dto.getLastName() != null) {
            existing.setLastName(dto.getLastName().toUpperCase().trim());
        }
        if (dto.getPassword() != null) {
            existing.setPassword(dto.getPassword().toUpperCase().trim());
        }
        if (dto.getUserType() != null) {
            existing.setUserType(dto.getUserType().toUpperCase().trim());
        }

        return userRepository.save(existing);
    }

    public void deleteUser(String userId) {
        User user = userRepository.findById(userId.toUpperCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userRepository.delete(user);
    }
}
