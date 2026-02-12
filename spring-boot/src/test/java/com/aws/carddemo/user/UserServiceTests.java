package com.aws.carddemo.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.aws.carddemo.exception.DuplicateKeyException;
import com.aws.carddemo.user.dto.UserCreateRequest;
import com.aws.carddemo.user.dto.UserUpdateRequest;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        appUserRepository.findByUserId("UNIT0001").ifPresent(u -> appUserRepository.deleteById(u.getId()));
    }

    @Test
    void createUserEncodesPassword() {
        userService.createUser(new UserCreateRequest("UNIT0001", "PlainPass1!", "Unit", "Tester", "U"));
        AppUser saved = appUserRepository.findByUserId("UNIT0001").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("PlainPass1!");
        assertThat(passwordEncoder.matches("PlainPass1!", saved.getPassword())).isTrue();
    }

    @Test
    void duplicateUserIdThrows() {
        userService.createUser(new UserCreateRequest("UNIT0001", "p1", "A", "B", "U"));
        assertThatThrownBy(() -> userService.createUser(new UserCreateRequest("UNIT0001", "p2", "A", "B", "U")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void updateUserChangesFields() {
        var created = userService.createUser(new UserCreateRequest("UNIT0001", "p1", "A", "B", "U"));
        var updated = userService.updateUser(created.id(), new UserUpdateRequest(null, "AA", null, "A"));
        assertThat(updated.firstName()).isEqualTo("AA");
        assertThat(updated.userType()).isEqualTo("A");
    }
}
