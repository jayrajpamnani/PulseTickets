package com.antra.platform.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Consistent, non-leaky error responses for all HTTP services. */
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
    return body(ex.getStatusCode(), ex.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .findFirst().orElse("Request validation failed");
    return body(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
  }

  private ResponseEntity<Map<String, Object>> body(org.springframework.http.HttpStatusCode status, String message) {
    return ResponseEntity.status(status).body(Map.of("timestamp", Instant.now(), "status", status.value(), "message", message == null ? "Request failed" : message));
  }
}
