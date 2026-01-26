package com.aws.carddemo.service;

import com.aws.carddemo.dto.UserDto;
import com.aws.carddemo.entity.User;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.mapper.UserMapper;
import com.aws.carddemo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserDto getUser(String userId) {
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userMapper.toDtoList(userRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAdminUsers() {
        return userMapper.toDtoList(userRepository.findAllAdmins());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getRegularUsers() {
        return userMapper.toDtoList(userRepository.findAllRegularUsers());
    }

    @Transactional(readOnly = true)
    public List<UserDto> searchByLastName(String lastName) {
        return userMapper.toDtoList(userRepository.searchByLastName(lastName));
    }

    @Transactional
    public UserDto createUser(UserDto dto) {
        User user = userMapper.toEntity(dto);
        user.setUserId(user.getUserId().toUpperCase());
        user.setUserPassword(passwordEncoder.encode(dto.getUserPassword()));
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(String userId, UserDto dto) {
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        userMapper.updateEntity(dto, user);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void updatePassword(String userId, String newPassword) {
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void enableUser(String userId) {
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableUser(String userId) {
        User user = userRepository.findByUserId(userId.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String userId) {
        if (!userRepository.existsByUserId(userId.toUpperCase())) {
            throw new ResourceNotFoundException("User", "userId", userId);
        }
        userRepository.deleteById(userId.toUpperCase());
    }

    @Transactional(readOnly = true)
    public long countAdminUsers() {
        return userRepository.countByType("A");
    }

    @Transactional(readOnly = true)
    public long countRegularUsers() {
        return userRepository.countByType("U");
    }

    @Transactional(readOnly = true)
    public boolean existsByUserId(String userId) {
        return userRepository.existsByUserId(userId.toUpperCase());
    }
}
