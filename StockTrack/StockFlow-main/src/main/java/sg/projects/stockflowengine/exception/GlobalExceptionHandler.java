package sg.projects.stockflowengine.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>Centralizes exception handling across the entire application using
 * {@link RestControllerAdvice}, eliminating the need for try-catch blocks
 * in individual controllers.
 *
 * <p>All error responses follow a consistent JSON structure:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Descriptive error message",
 *   "timestamp": "2026-07-13T10:30:00"
 * }
 * </pre>
 *
 * <p>Exception handling priority (most specific to least specific):
 * <ol>
 *   <li>{@link AuthenticationException} → 401 Unauthorized</li>
 *   <li>{@link DataIntegrityViolationException} → 400 Duplicate Entry</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Validation Failed</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 *   <li>{@link RuntimeException} → 404 Not Found</li>
 *   <li>{@link Exception} → 500 Internal Server Error</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link jakarta.validation.Valid} annotation validation failures.
     *
     * <p>Returns field-level error messages for all failed validations,
     * allowing clients to identify exactly which fields are invalid.
     *
     * @param ex the validation exception containing field error details
     * @return 400 Bad Request with map of field names to error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        response.put("status", 400);
        response.put("error", "Validation Failed");
        response.put("messages", fieldErrors);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles business logic violations thrown by service layer.
     *
     * <p>Used for domain rule violations such as:
     * <ul>
     *   <li>Insufficient stock on outward creation</li>
     *   <li>Invalid status transitions on inward/outward</li>
     *   <li>INACTIVE variant or customer guards</li>
     *   <li>Soft delete protection violations</li>
     *   <li>Invalid role assignment</li>
     * </ul>
     *
     * @param ex the illegal argument exception with business rule message
     * @return 400 Bad Request with descriptive error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles entity not found exceptions thrown by service layer.
     *
     * <p>Catches {@link RuntimeException} thrown when a requested entity
     * does not exist in the database (e.g. "Material not found with id: 5").
     *
     * @param ex the runtime exception with not found message
     * @return 404 Not Found with descriptive error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 404);
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles all unexpected exceptions not caught by more specific handlers.
     *
     * <p>Returns a generic error message to avoid exposing internal
     * system details or stack traces to API consumers.
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 500);
        response.put("error", "Internal Server Error");
        response.put("message", "Something went wrong. Please try again.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles database unique constraint violations.
     *
     * <p>Triggered when attempting to insert a duplicate record that
     * violates a unique constraint, such as:
     * <ul>
     *   <li>Duplicate material name</li>
     *   <li>Duplicate variant name within the same parent material</li>
     * </ul>
     *
     * @param ex the data integrity violation exception from PostgreSQL
     * @return 400 Bad Request with duplicate entry message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("error", "Duplicate Entry");
        response.put("message", "A record with the same name already exists.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles authentication failures during login.
     *
     * <p>Triggered by {@link sg.projects.stockflowengine.controller.AuthController}
     * when provided credentials do not match any active user account.
     * Returns a generic message to avoid revealing which field is incorrect.
     *
     * @param ex the authentication exception from Spring Security
     * @return 401 Unauthorized with generic invalid credentials message
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 401);
        response.put("error", "Unauthorized");
        response.put("message", "Invalid username or password.");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}