package com.carddemo.service;

import com.carddemo.dto.UserCreateRequest;
import com.carddemo.entity.UserSecurity;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.UserSecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserSecurityRepository userSecurityRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    private UserSecurity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSecurity();
        testUser.setUsrId("USER0001");
        testUser.setUsrFname("FIRST");
        testUser.setUsrLname("USER");
        testUser.setUsrPwd("USER0001");
        testUser.setUsrType("U");
    }

    @Test
    void createUserSuccess() {
        when(userSecurityRepository.existsById("NEWUSER")).thenReturn(false);
        when(userSecurityRepository.save(any(UserSecurity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserCreateRequest request = new UserCreateRequest();
        request.setUserId("newuser");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPassword("newpass");
        request.setUserType("U");

        UserSecurity result = userAdminService.createUser(request);

        assertNotNull(result);
        assertEquals("NEWUSER", result.getUsrId());
        assertEquals("U", result.getUsrType());
    }

    @Test
    void createUserAlreadyExists() {
        when(userSecurityRepository.existsById("USER0001")).thenReturn(true);

        UserCreateRequest request = new UserCreateRequest();
        request.setUserId("user0001");
        request.setFirstName("First");
        request.setLastName("User");
        request.setPassword("pass");
        request.setUserType("U");

        assertThrows(IllegalArgumentException.class,
                () -> userAdminService.createUser(request));
    }

    @Test
    void getUserNotFound() {
        when(userSecurityRepository.findById("BADUSER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userAdminService.getUser("baduser"));
    }

    @Test
    void deleteUserSuccess() {
        when(userSecurityRepository.existsById("USER0001")).thenReturn(true);

        userAdminService.deleteUser("user0001");

        verify(userSecurityRepository).deleteById("USER0001");
    }
}
