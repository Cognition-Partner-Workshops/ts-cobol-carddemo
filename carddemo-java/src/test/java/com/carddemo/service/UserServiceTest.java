package com.carddemo.service;

import com.carddemo.dto.UserDto;
import com.carddemo.entity.User;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.exception.ValidationException;
import com.carddemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("USER0001");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("pass1234");
        testUser.setUserType("R");
    }

    @Test
    void authenticate_validCredentials_returnsUser() {
        testUser.setPassword("PASS1234");
        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));

        User result = userService.authenticate("USER0001", "pass1234");

        assertNotNull(result);
        assertEquals("USER0001", result.getUserId());
    }

    @Test
    void authenticate_invalidPassword_throwsValidationException() {
        testUser.setPassword("PASS1234");
        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));

        assertThrows(ValidationException.class,
                () -> userService.authenticate("USER0001", "wrongpass"));
    }

    @Test
    void authenticate_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.authenticate("UNKNOWN", "pass1234"));
    }

    @Test
    void listUsers_returnsPagedResults() {
        Page<User> page = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<User> result = userService.listUsers(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("USER0001", result.getContent().get(0).getUserId());
    }

    @Test
    void getUser_existingUser_returnsUser() {
        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));

        User result = userService.getUser("USER0001");

        assertEquals("John", result.getFirstName());
    }

    @Test
    void getUser_nonExistent_throwsResourceNotFoundException() {
        when(userRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUser("UNKNOWN"));
    }

    @Test
    void addUser_validDto_createsUser() {
        UserDto dto = new UserDto();
        dto.setUserId("NEWUSER1");
        dto.setFirstName("Jane");
        dto.setLastName("Smith");
        dto.setPassword("newpass1");
        dto.setUserType("R");

        when(userRepository.findById("NEWUSER1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.addUser(dto);

        assertNotNull(result);
        assertEquals("NEWUSER1", result.getUserId());
        assertEquals("JANE", result.getFirstName());
    }

    @Test
    void addUser_duplicateId_throwsValidationException() {
        UserDto dto = new UserDto();
        dto.setUserId("USER0001");
        dto.setPassword("pass1234");

        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));

        assertThrows(ValidationException.class, () -> userService.addUser(dto));
    }

    @Test
    void updateUser_existingUser_updatesFields() {
        UserDto dto = new UserDto();
        dto.setFirstName("Updated");
        dto.setLastName("Name");
        dto.setPassword("newpass1");
        dto.setUserType("A");

        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser("USER0001", dto);

        assertEquals("UPDATED", result.getFirstName());
        assertEquals("NAME", result.getLastName());
        assertEquals("A", result.getUserType());
    }

    @Test
    void deleteUser_existingUser_deletesSuccessfully() {
        when(userRepository.findById("USER0001")).thenReturn(Optional.of(testUser));

        userService.deleteUser("USER0001");

        verify(userRepository).delete(testUser);
    }
}
