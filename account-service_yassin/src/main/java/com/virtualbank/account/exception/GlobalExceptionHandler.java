package com.virtualbank.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleAccountNotFoundException(AccountNotFoundException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.NOT_FOUND.value());
    error.put("error", "Not Found");
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientFundsException(InsufficientFundsException ex) {
    Map<String, Object> error = new HashMap<>();
    error.put("status", HttpStatus.BAD_REQUEST.value());
    error.put("error", "Bad Request");
    error.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }
  @ExceptionHandler({
          MethodArgumentNotValidException.class,
          MethodArgumentTypeMismatchException.class,
          HttpMessageNotReadableException.class
  })
  public ResponseEntity<String> handleInvalidBalanceOrType(Exception ex) {
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Invalid initial balance or account type");
  }
}