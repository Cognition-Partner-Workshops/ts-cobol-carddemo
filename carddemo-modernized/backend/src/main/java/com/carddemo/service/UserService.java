package com.carddemo.service;

import com.carddemo.dto.RegisterRequest;
import com.carddemo.exception.BadRequestException;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.model.User;
import com.carddemo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
    }
    
    public User createUser(RegisterRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new BadRequestException("User with ID " + request.getUserId() + " already exists");
        }
        
        User user = User.builder()
                .userId(request.getUserId().toUpperCase())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(request.getUserType())
                .build();
        
        return userRepository.save(user);
    }
    
    public User updateUser(String userId, User userDetails) {
        User user = getUserById(userId);
        
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        user.setUserType(userDetails.getUserType());
        
        return userRepository.save(user);
    }
    
    public void deleteUser(String userId) {
        if (!userRepository.existsByUserId(userId)) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.deleteByUserId(userId);
    }
}
