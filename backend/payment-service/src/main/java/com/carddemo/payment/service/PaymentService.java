package com.carddemo.payment.service;

import com.carddemo.common.dto.PagedResponse;
import com.carddemo.common.exception.BadRequestException;
import com.carddemo.common.exception.ResourceNotFoundException;
import com.carddemo.payment.dto.CreatePaymentRequest;
import com.carddemo.payment.dto.PaymentDto;
import com.carddemo.payment.entity.Payment;
import com.carddemo.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PagedResponse<PaymentDto> getAllPayments(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Payment> paymentPage = paymentRepository.findAll(pageable);

        List<PaymentDto> payments = paymentPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PaymentDto>builder()
                .content(payments)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .build();
    }

    public PaymentDto getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        return mapToDto(payment);
    }

    public PagedResponse<PaymentDto> getPaymentsByAccountId(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByAccountId(accountId, pageable);

        List<PaymentDto> payments = paymentPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponse.<PaymentDto>builder()
                .content(payments)
                .page(paymentPage.getNumber())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .first(paymentPage.isFirst())
                .last(paymentPage.isLast())
                .build();
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        String status = request.getScheduledDate() != null && request.getScheduledDate().isAfter(LocalDateTime.now())
                ? "SCHEDULED"
                : "PENDING";

        Payment payment = Payment.builder()
                .paymentId(generatePaymentId())
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .sourceAccount(request.getSourceAccount())
                .routingNumber(request.getRoutingNumber())
                .confirmationNumber(generateConfirmationNumber())
                .status(status)
                .scheduledDate(request.getScheduledDate())
                .notes(request.getNotes())
                .build();

        payment = paymentRepository.save(payment);

        if ("PENDING".equals(status)) {
            payment = processPayment(payment);
        }

        return mapToDto(payment);
    }

    @Transactional
    public PaymentDto processPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        if (!"PENDING".equals(payment.getStatus()) && !"SCHEDULED".equals(payment.getStatus())) {
            throw new BadRequestException("Payment cannot be processed. Current status: " + payment.getStatus());
        }

        payment = processPayment(payment);
        return mapToDto(payment);
    }

    @Transactional
    public PaymentDto cancelPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        if ("COMPLETED".equals(payment.getStatus())) {
            throw new BadRequestException("Cannot cancel a completed payment");
        }

        payment.setStatus("CANCELLED");
        payment = paymentRepository.save(payment);
        return mapToDto(payment);
    }

    public List<PaymentDto> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PaymentDto> getPaymentsByAccountAndDateRange(Long accountId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        return paymentRepository.findByAccountIdAndDateRange(accountId, startDateTime, endDateTime).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalPaymentsByAccount(Long accountId) {
        BigDecimal total = paymentRepository.sumCompletedPaymentsByAccountId(accountId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional
    public int processScheduledPayments() {
        List<Payment> duePayments = paymentRepository.findPaymentsDueForProcessing(LocalDateTime.now());
        int processed = 0;
        for (Payment payment : duePayments) {
            try {
                processPayment(payment);
                processed++;
            } catch (Exception e) {
                payment.setStatus("FAILED");
                payment.setNotes("Processing failed: " + e.getMessage());
                paymentRepository.save(payment);
            }
        }
        return processed;
    }

    private Payment processPayment(Payment payment) {
        payment.setStatus("COMPLETED");
        payment.setProcessedDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private String generatePaymentId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateConfirmationNumber() {
        return "CNF" + System.currentTimeMillis();
    }

    private PaymentDto mapToDto(Payment payment) {
        String maskedSourceAccount = null;
        if (payment.getSourceAccount() != null && payment.getSourceAccount().length() >= 4) {
            maskedSourceAccount = "****" + payment.getSourceAccount().substring(payment.getSourceAccount().length() - 4);
        }

        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .accountId(payment.getAccountId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .sourceAccountMasked(maskedSourceAccount)
                .confirmationNumber(payment.getConfirmationNumber())
                .status(payment.getStatus())
                .scheduledDate(payment.getScheduledDate())
                .processedDate(payment.getProcessedDate())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
