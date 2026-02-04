package com.aws.carddemo.api.exception;

import com.aws.carddemo.service.account.AccountService;
import com.aws.carddemo.service.account.BillPaymentService;
import com.aws.carddemo.service.card.CardService;
import com.aws.carddemo.service.transaction.TransactionService;
import com.aws.carddemo.service.transactiontype.TransactionTypeService;
import com.aws.carddemo.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountService.AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountService.AccountNotFoundException ex) {
        log.warn("Account not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CardService.CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFound(CardService.CardNotFoundException ex) {
        log.warn("Card not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CardService.CardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExists(CardService.CardAlreadyExistsException ex) {
        log.warn("Card already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TransactionService.TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionService.TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransactionService.CardNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleCardNotActive(TransactionService.CardNotActiveException ex) {
        log.warn("Card not active: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TransactionService.CardExpiredException.class)
    public ResponseEntity<ErrorResponse> handleCardExpired(TransactionService.CardExpiredException ex) {
        log.warn("Card expired: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TransactionService.CreditLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleCreditLimitExceeded(TransactionService.CreditLimitExceededException ex) {
        log.warn("Credit limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserService.UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserService.UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserService.UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TransactionTypeService.TransactionTypeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionTypeNotFound(TransactionTypeService.TransactionTypeNotFoundException ex) {
        log.warn("Transaction type not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransactionTypeService.TransactionTypeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTransactionTypeAlreadyExists(TransactionTypeService.TransactionTypeAlreadyExistsException ex) {
        log.warn("Transaction type already exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BillPaymentService.AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleBillPaymentAccountNotActive(BillPaymentService.AccountNotActiveException ex) {
        log.warn("Account not active for bill payment: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BillPaymentService.InvalidPaymentAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentAmount(BillPaymentService.InvalidPaymentAmountException ex) {
        log.warn("Invalid payment amount: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BillPaymentService.NoActiveCardException.class)
    public ResponseEntity<ErrorResponse> handleNoActiveCard(BillPaymentService.NoActiveCardException ex) {
        log.warn("No active card: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }
}
