package com.aws.carddemo.service;

import com.aws.carddemo.dto.AuthRequestDto;
import com.aws.carddemo.entity.Account;
import com.aws.carddemo.entity.AuthFraudDetection;
import com.aws.carddemo.entity.AuthRequest;
import com.aws.carddemo.entity.CardXref;
import com.aws.carddemo.exception.ResourceNotFoundException;
import com.aws.carddemo.repository.AccountRepository;
import com.aws.carddemo.repository.AuthFraudDetectionRepository;
import com.aws.carddemo.repository.AuthRequestRepository;
import com.aws.carddemo.repository.CardXrefRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private final AuthRequestRepository authRequestRepository;
    private final AuthFraudDetectionRepository fraudDetectionRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;

    public AuthorizationService(AuthRequestRepository authRequestRepository,
                                 AuthFraudDetectionRepository fraudDetectionRepository,
                                 CardXrefRepository cardXrefRepository,
                                 AccountRepository accountRepository) {
        this.authRequestRepository = authRequestRepository;
        this.fraudDetectionRepository = fraudDetectionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AuthRequestDto processAuthorizationRequest(AuthRequestDto dto) {
        AuthRequest authRequest = AuthRequest.builder()
                .cardNum(dto.getCardNum())
                .tranAmt(dto.getTranAmt())
                .merchantId(dto.getMerchantId())
                .merchantName(dto.getMerchantName())
                .merchantCity(dto.getMerchantCity())
                .merchantZip(dto.getMerchantZip())
                .authStatus(AuthRequest.STATUS_PENDING)
                .requestTs(LocalDateTime.now())
                .build();

        authRequest = authRequestRepository.save(authRequest);

        String declineReason = validateAuthorization(dto);
        
        if (declineReason == null) {
            authRequest.setAuthStatus(AuthRequest.STATUS_APPROVED);
            authRequest.setAuthCode(generateAuthCode());
        } else {
            authRequest.setAuthStatus(AuthRequest.STATUS_DECLINED);
            authRequest.setDeclineReason(declineReason);
        }
        
        authRequest.setResponseTs(LocalDateTime.now());
        authRequest = authRequestRepository.save(authRequest);

        checkForFraud(authRequest);

        return toDto(authRequest);
    }

    private String validateAuthorization(AuthRequestDto dto) {
        CardXref xref = cardXrefRepository.findByCardNumWithDetails(dto.getCardNum()).orElse(null);
        
        if (xref == null) {
            return "INVALID CARD NUMBER";
        }

        Account account = xref.getAccount();
        if (account == null) {
            return "ACCOUNT NOT FOUND";
        }

        if (!account.isActive()) {
            return "ACCOUNT INACTIVE";
        }

        if (account.isExpired()) {
            return "ACCOUNT EXPIRED";
        }

        BigDecimal projectedBalance = account.getProjectedBalance(dto.getTranAmt());
        if (account.getAcctCreditLimit().compareTo(projectedBalance) < 0) {
            return "CREDIT LIMIT EXCEEDED";
        }

        return null;
    }

    private void checkForFraud(AuthRequest authRequest) {
        BigDecimal fraudScore = calculateFraudScore(authRequest);
        
        AuthFraudDetection fraudDetection = AuthFraudDetection.builder()
                .cardNum(authRequest.getCardNum())
                .authRequest(authRequest)
                .fraudScore(fraudScore)
                .isFraud(fraudScore.compareTo(new BigDecimal("80.00")) >= 0)
                .reviewed(false)
                .build();

        fraudDetectionRepository.save(fraudDetection);
    }

    private BigDecimal calculateFraudScore(AuthRequest authRequest) {
        BigDecimal score = BigDecimal.ZERO;
        
        if (authRequest.getTranAmt().compareTo(new BigDecimal("5000.00")) > 0) {
            score = score.add(new BigDecimal("30.00"));
        }
        
        return score;
    }

    private String generateAuthCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional(readOnly = true)
    public AuthRequestDto getAuthRequest(Long authId) {
        AuthRequest authRequest = authRequestRepository.findById(authId)
                .orElseThrow(() -> new ResourceNotFoundException("AuthRequest", "authId", authId));
        return toDto(authRequest);
    }

    @Transactional(readOnly = true)
    public Page<AuthRequestDto> getAuthRequestsByCard(String cardNum, Pageable pageable) {
        return authRequestRepository.findByCardNum(cardNum, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<AuthRequestDto> getAuthRequestsByStatus(String status, Pageable pageable) {
        return authRequestRepository.findByAuthStatus(status, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<AuthRequestDto> getPendingAuthRequests() {
        return authRequestRepository.findPendingRequests().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AuthRequestDto toDto(AuthRequest authRequest) {
        return AuthRequestDto.builder()
                .authId(authRequest.getAuthId())
                .cardNum(authRequest.getCardNum())
                .tranAmt(authRequest.getTranAmt())
                .merchantId(authRequest.getMerchantId())
                .merchantName(authRequest.getMerchantName())
                .merchantCity(authRequest.getMerchantCity())
                .merchantZip(authRequest.getMerchantZip())
                .authStatus(authRequest.getAuthStatus())
                .authCode(authRequest.getAuthCode())
                .declineReason(authRequest.getDeclineReason())
                .requestTs(authRequest.getRequestTs())
                .responseTs(authRequest.getResponseTs())
                .createdAt(authRequest.getCreatedAt())
                .build();
    }
}
