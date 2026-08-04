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
  ResponseEntity<ApiErrorResponse> status(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    String reasonPhrase = status != null ? status.getReasonPhrase() : "Error";
    return body(ex.getStatusCode(), reasonPhrase, ex.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .findFirst().orElse("Request validation failed");
    return body(HttpStatus.BAD_REQUEST, "Bad Request", message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException ex) {
    return body(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> unexpected(Exception ex) {
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected server error");
  }

  private ResponseEntity<ApiErrorResponse> body(org.springframework.http.HttpStatusCode status, String error, String message) {
    ApiErrorResponse response = new ApiErrorResponse(status.value(), error, message == null ? "Request failed" : message);
    return ResponseEntity.status(status).body(response);
  }
}
