package com.carddemo.payment.service;

import com.carddemo.common.dto.PageResponse;
import com.carddemo.common.exception.BusinessException;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.payment.dto.PaymentDto;
import com.carddemo.payment.dto.PaymentRequest;
import com.carddemo.payment.entity.Payment;
import com.carddemo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentDto getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        return mapToPaymentDto(payment);
    }

    public PageResponse<PaymentDto> getPaymentsByAccount(String accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByAccountId(accountId, pageable);

        List<PaymentDto> payments = paymentPage.getContent().stream()
                .map(this::mapToPaymentDto)
                .collect(Collectors.toList());

        return PageResponse.<PaymentDto>builder()
                .content(payments)
                .pageNumber(paymentPage.getNumber())
                .pageSize(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .build();
    }

    @Transactional
    public PaymentDto createPayment(PaymentRequest request) {
        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero", "INVALID_AMOUNT");
        }

        String paymentId = generatePaymentId();
        String confirmationNumber = generateConfirmationNumber();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .paymentSource(request.getPaymentSource())
                .sourceAccount(request.getSourceAccount())
                .confirmationNumber(confirmationNumber)
                .status("PENDING")
                .scheduledDate(request.getScheduledDate() != null ? request.getScheduledDate() : LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentDto(savedPayment);
    }

    @Transactional
    public PaymentDto processPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        if (!"PENDING".equals(payment.getStatus())) {
            throw new BusinessException("Payment is not in PENDING status", "INVALID_STATUS");
        }

        payment.setStatus("PROCESSED");
        payment.setProcessedDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentDto(savedPayment);
    }

    @Transactional
    public PaymentDto cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        if (!"PENDING".equals(payment.getStatus())) {
            throw new BusinessException("Only PENDING payments can be cancelled", "INVALID_STATUS");
        }

        payment.setStatus("CANCELLED");
        Payment savedPayment = paymentRepository.save(payment);
        return mapToPaymentDto(savedPayment);
    }

    private PaymentDto mapToPaymentDto(Payment payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .accountId(payment.getAccountId())
                .amount(payment.getAmount())
                .paymentSource(payment.getPaymentSource())
                .sourceAccount(payment.getSourceAccount())
                .confirmationNumber(payment.getConfirmationNumber())
                .status(payment.getStatus())
                .scheduledDate(payment.getScheduledDate())
                .processedDate(payment.getProcessedDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String generatePaymentId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().substring(0, 2).toUpperCase();
        return timestamp + random;
    }

    private String generateConfirmationNumber() {
        return "CONF" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}
