package com.carddemo.repository;

import com.carddemo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("USER0001");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("password");
        testUser.setUserType("U");

        adminUser = new User();
        adminUser.setUserId("ADMIN001");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPassword("adminpwd");
        adminUser.setUserType("A");
    }

    @Test
    void testSaveAndFindById() {
        User saved = userRepository.save(testUser);
        
        Optional<User> found = userRepository.findById(saved.getUserId());
        
        assertTrue(found.isPresent());
        assertEquals(testUser.getFirstName(), found.get().getFirstName());
    }

    @Test
    void testFindByUserIdIgnoreCase() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByUserIdIgnoreCase("user0001");
        
        assertTrue(found.isPresent());
        assertEquals(testUser.getUserId(), found.get().getUserId());
    }

    @Test
    void testFindByUserType() {
        userRepository.save(testUser);
        userRepository.save(adminUser);
        
        List<User> regularUsers = userRepository.findByUserType("U");
        List<User> adminUsers = userRepository.findByUserType("A");
        
        assertEquals(1, regularUsers.size());
        assertEquals(1, adminUsers.size());
    }

    @Test
    void testFindAllAdmins() {
        userRepository.save(testUser);
        userRepository.save(adminUser);
        
        List<User> admins = userRepository.findAllAdmins();
        
        assertEquals(1, admins.size());
        assertEquals("ADMIN001", admins.get(0).getUserId());
    }

    @Test
    void testFindAllRegularUsers() {
        userRepository.save(testUser);
        userRepository.save(adminUser);
        
        List<User> regularUsers = userRepository.findAllRegularUsers();
        
        assertEquals(1, regularUsers.size());
        assertEquals("USER0001", regularUsers.get(0).getUserId());
    }

    @Test
    void testFindByLastName() {
        userRepository.save(testUser);
        
        List<User> users = userRepository.findByLastName("Doe");
        
        assertEquals(1, users.size());
    }

    @Test
    void testExistsByUserId() {
        userRepository.save(testUser);
        
        assertTrue(userRepository.existsByUserId("USER0001"));
        assertFalse(userRepository.existsByUserId("NONEXIST"));
    }

    @Test
    void testCountByUserType() {
        userRepository.save(testUser);
        userRepository.save(adminUser);
        
        long regularCount = userRepository.countByUserType("U");
        long adminCount = userRepository.countByUserType("A");
        
        assertEquals(1, regularCount);
        assertEquals(1, adminCount);
    }
}
