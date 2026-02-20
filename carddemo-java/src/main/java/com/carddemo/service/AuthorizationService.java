package com.carddemo.service;

import com.carddemo.entity.PendingAuthorization;
import com.carddemo.exception.ResourceNotFoundException;
import com.carddemo.repository.PendingAuthorizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthorizationService {

    private final PendingAuthorizationRepository pendingAuthorizationRepository;

    public AuthorizationService(PendingAuthorizationRepository pendingAuthorizationRepository) {
        this.pendingAuthorizationRepository = pendingAuthorizationRepository;
    }

    public Page<PendingAuthorization> listPendingAuthorizations(Pageable pageable) {
        return pendingAuthorizationRepository.findAll(pageable);
    }

    public Page<PendingAuthorization> listByAccount(Long acctId, Pageable pageable) {
        return pendingAuthorizationRepository.findByAcctId(acctId, pageable);
    }

    public PendingAuthorization getAuthorization(Long id) {
        return pendingAuthorizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authorization not found: " + id));
    }

    @Transactional
    public PendingAuthorization processAuthorization(PendingAuthorization auth) {
        auth.setAuthDate(LocalDateTime.now());
        auth.setExpiryDate(LocalDateTime.now().plusDays(7));
        auth.setAuthStatus("PENDING");
        auth.setFraudFlag(false);
        return pendingAuthorizationRepository.save(auth);
    }

    @Transactional
    public PendingAuthorization markAsFraud(Long id) {
        PendingAuthorization auth = pendingAuthorizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authorization not found: " + id));
        auth.setFraudFlag(true);
        auth.setAuthStatus("FRAUD");
        return pendingAuthorizationRepository.save(auth);
    }

    @Transactional
    public int purgeExpiredAuthorizations() {
        List<PendingAuthorization> expired =
                pendingAuthorizationRepository.findByExpiryDateBefore(LocalDateTime.now());
        int count = expired.size();
        pendingAuthorizationRepository.deleteAll(expired);
        return count;
    }
}
