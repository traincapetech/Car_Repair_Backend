package com.carservice.backend.common.exception;

import com.carservice.backend.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFound(
                        ResourceNotFoundException exception) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(
                                                new ApiResponse<>(
                                                                false,
                                                                exception.getMessage(),
                                                                null));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(
                        Exception exception) {

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(
                                                new ApiResponse<>(
                                                                false,
                                                                "Something went wrong",
                                                                null));
        }

        @ExceptionHandler(UserAlreadyExistsException.class)
        public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
                        UserAlreadyExistsException exception) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(
                                                new ApiResponse<>(
                                                                false,
                                                                exception.getMessage(),
                                                                null));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
                        MethodArgumentNotValidException exception) {

                Map<String, String> errors = new HashMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(
                                                error.getField(),
                                                error.getDefaultMessage()));

                return ResponseEntity
                                .badRequest()
                                .body(
                                                new ApiResponse<>(
                                                                false,
                                                                "Validation failed",
                                                                errors));
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
                        InvalidCredentialsException ex) {

                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(
                                                ApiResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
                        AccessDeniedException exception) {

                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(
                                                ApiResponse.error(
                                                                "Access denied"));
        }
}
