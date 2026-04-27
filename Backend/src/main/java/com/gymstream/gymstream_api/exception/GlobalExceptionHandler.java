package com.gymstream.gymstream_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Manejador global de excepciones - retorna respuestas consistentes para errores
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Manejar argumentos no válidos (validaciones de @NotBlank, @Size, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validación fallida");
        response.put("message", "Los datos enviados no son válidos");
        
        // Recopilar todos los errores de validación
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        response.put("details", errors);
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Manejar argumentos inválidos (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Argumento inválido");
        response.put("message", ex.getMessage());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Manejar ResponseStatusException (errores con status HTTP específico)
    // Esto captura los errores que lanzamos con HttpStatus.CONFLICT, 
    // HttpStatus.NOT_FOUND, etc. desde los Services
    // Es importante que esté ANTES del handler de RuntimeException
    // porque ResponseStatusException extiende RuntimeException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", ex.getStatusCode().value());
        response.put("error", ex.getReason());
        response.put("message", ex.getReason());

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestHeader(
            MissingRequestHeaderException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Header requerido");
        response.put("message", "Falta el header " + ex.getHeaderName());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Parametro invalido");
        response.put("message", "El parametro " + ex.getName() + " tiene un formato invalido");

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Manejar errores generales de ejecución (RuntimeException)
    // Esto incluye todos los errores no capturados por handlers anteriores
    // Intenta inferir el código HTTP basado en el mensaje
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        
        // Si el mensaje contiene "no encontrada", es un error 404
        if (ex.getMessage() != null && ex.getMessage().contains("no encontrada")) {
            response.put("status", HttpStatus.NOT_FOUND.value());
            response.put("error", "Recurso no encontrado");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        
        // Si el mensaje contiene "API" o "conectar", es un error de servicio externo
        if (ex.getMessage() != null && 
            (ex.getMessage().contains("API") || ex.getMessage().contains("conectar"))) {
            response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            response.put("error", "Servicio no disponible");
        } else {
            response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("error", "Error interno del servidor");
        }
        
        response.put("message", ex.getMessage());
        
        return new ResponseEntity<>(response, 
            ex.getMessage() != null && 
            (ex.getMessage().contains("API") || ex.getMessage().contains("conectar"))
            ? HttpStatus.SERVICE_UNAVAILABLE 
            : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Manejar cualquier otra excepción no capturada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Error inesperado");
        response.put("message", "Ha ocurrido un error inesperado en el servidor");
        
        // En producción, no deberías devolver detalles del error
        // response.put("details", ex.getMessage());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
