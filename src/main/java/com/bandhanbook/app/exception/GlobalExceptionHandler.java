package com.bandhanbook.app.exception;

import com.bandhanbook.app.payload.response.base.ApiResponse;
import com.bandhanbook.app.payload.response.base.CommonApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static com.bandhanbook.app.utilities.ErrorResponseMessages.VALIDATION_ERROR;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PhoneNumberNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleRuntime(PhoneNumberNotFoundException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build()));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleRuntime(RuntimeException ex) {
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build()));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleAll(Exception ex) {
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .build()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleValidationExceptions(WebExchangeBindException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.<Map<String, String>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(VALIDATION_ERROR)
                .error(errors)
                .build()));
    }

    @ExceptionHandler(CommontException.class)
    public Mono<ResponseEntity<CommonApiResponse<String>>> handleRuntime(CommontException ex) {
        return Mono.just(ResponseEntity.badRequest().body(CommonApiResponse.<String>builder().
                status(HttpStatus.BAD_REQUEST.value()).
                error(ex.getMessage()).build()));
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public Mono<ResponseEntity<CommonApiResponse<String>>> handleRuntime(UnAuthorizedException ex) {
        return Mono.just(ResponseEntity.badRequest().body(CommonApiResponse.<String>builder().
                status(HttpStatus.UNAUTHORIZED.value()).
                error(ex.getMessage()).build()));
    }
}
