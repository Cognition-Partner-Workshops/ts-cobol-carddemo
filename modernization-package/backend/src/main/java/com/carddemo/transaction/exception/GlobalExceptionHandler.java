package com.carddemo.transaction.exception;

import com.carddemo.transaction.dto.ErrorResponse;
import com.carddemo.transaction.dto.ValidationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Transaction Processing API.
 * Maps custom exceptions to proper HTTP responses with legacy error message parity.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        String errorLabel = ex.getHttpStatus() == 404 ? "Not Found" : "Validation Failed";
        ValidationErrorResponse response = new ValidationErrorResponse(
                ex.getHttpStatus(),
                errorLabel,
                ex.getMessage(),
                ex.getField(),
                ex.getBusinessRule(),
                ex.getPhase()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
                404,
                "Not Found",
                ex.getMessage(),
                ex.getField(),
                ex.getBusinessRule()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransactionException(DuplicateTransactionException ex) {
        ErrorResponse response = new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage(),
                "transactionId",
                "BR-AT-14"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(
                400,
                "Bad Request",
                ex.getMessage(),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
