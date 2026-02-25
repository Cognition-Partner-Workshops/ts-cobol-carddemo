package com.cardemo.service;

import com.cardemo.dto.UserRequest;
import com.cardemo.entity.CardDemoUser;
import com.cardemo.exception.CardDemoException;
import com.cardemo.repository.CardDemoUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User management service (Admin only).
 * Migrated from COUSR00C (CU00 - list), COUSR01C (CU01 - add),
 * COUSR02C (CU02 - update), COUSR03C (CU03 - delete).
 */
@Service
public class UserManagementService {

    private final CardDemoUserRepository userRepository;

    public UserManagementService(CardDemoUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * List users - migrated from COUSR00C (CU00 transaction).
     * COBOL: EXEC CICS STARTBR DATASET(WS-USRSEC-FILE) RIDFLD(WS-USR-ID)
     */
    public Page<CardDemoUser> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get user by ID.
     */
    public CardDemoUser getUser(String usrId) {
        return userRepository.findById(usrId.toUpperCase())
                .orElseThrow(() -> CardDemoException.notFound("User not found: " + usrId));
    }

    /**
     * Add user - migrated from COUSR01C (CU01 transaction).
     * COBOL: PROCESS-ENTER-KEY -> WRITE-USER-SEC-FILE
     * EXEC CICS WRITE DATASET(WS-USRSEC-FILE) FROM(SEC-USER-DATA)
     */
    @Transactional
    public CardDemoUser createUser(UserRequest request) {
        String userId = request.getUsrId().toUpperCase();

        // Check if user already exists - COBOL: RESP(WS-RESP-CD) checks for DUPREC
        if (userRepository.existsById(userId)) {
            throw CardDemoException.conflict("User already exists: " + userId);
        }

        // Validate user type - COBOL: IF FLG-USRTYPE-NOT-OK
        String userType = request.getUsrType() != null ? request.getUsrType().toUpperCase() : "R";
        if (!"A".equals(userType) && !"R".equals(userType)) {
            throw CardDemoException.badRequest("User type must be 'A' (Admin) or 'R' (Regular)");
        }

        CardDemoUser user = new CardDemoUser();
        user.setUsrId(userId);
        user.setUsrFname(request.getUsrFname() != null ? request.getUsrFname().toUpperCase() : "");
        user.setUsrLname(request.getUsrLname() != null ? request.getUsrLname().toUpperCase() : "");
        user.setUsrPwd(request.getUsrPwd() != null ? request.getUsrPwd().toUpperCase() : "PASSWORD");
        user.setUsrType(userType);

        return userRepository.save(user);
    }

    /**
     * Update user - migrated from COUSR02C (CU02 transaction).
     * COBOL: PROCESS-ENTER-KEY -> REWRITE-USER-SEC-FILE
     * EXEC CICS REWRITE DATASET(WS-USRSEC-FILE) FROM(SEC-USER-DATA)
     */
    @Transactional
    public CardDemoUser updateUser(String usrId, UserRequest request) {
        CardDemoUser existing = userRepository.findById(usrId.toUpperCase())
                .orElseThrow(() -> CardDemoException.notFound("User not found: " + usrId));

        if (request.getUsrFname() != null) {
            existing.setUsrFname(request.getUsrFname().toUpperCase());
        }
        if (request.getUsrLname() != null) {
            existing.setUsrLname(request.getUsrLname().toUpperCase());
        }
        if (request.getUsrPwd() != null) {
            existing.setUsrPwd(request.getUsrPwd().toUpperCase());
        }
        if (request.getUsrType() != null) {
            String userType = request.getUsrType().toUpperCase();
            if (!"A".equals(userType) && !"R".equals(userType)) {
                throw CardDemoException.badRequest("User type must be 'A' (Admin) or 'R' (Regular)");
            }
            existing.setUsrType(userType);
        }

        return userRepository.save(existing);
    }

    /**
     * Delete user - migrated from COUSR03C (CU03 transaction).
     * COBOL: PROCESS-ENTER-KEY -> DELETE-USER-SEC-FILE
     * EXEC CICS DELETE DATASET(WS-USRSEC-FILE) RIDFLD(WS-USR-ID)
     */
    @Transactional
    public void deleteUser(String usrId) {
        if (!userRepository.existsById(usrId.toUpperCase())) {
            throw CardDemoException.notFound("User not found: " + usrId);
        }
        userRepository.deleteById(usrId.toUpperCase());
    }
}
