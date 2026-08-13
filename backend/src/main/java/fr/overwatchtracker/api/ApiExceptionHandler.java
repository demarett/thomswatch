package fr.overwatchtracker.api;

import fr.overwatchtracker.dto.PlayerDtos.ApiError;
import fr.overwatchtracker.integration.OverfastException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(OverfastException.class) ResponseEntity<ApiError> overfast(OverfastException e){
    HttpStatus status=switch(e.code()){case "PLAYER_NOT_FOUND"->HttpStatus.NOT_FOUND;case "RATE_LIMIT","UPSTREAM_RATE_LIMIT"->HttpStatus.TOO_MANY_REQUESTS;case "UPSTREAM_TIMEOUT"->HttpStatus.GATEWAY_TIMEOUT;default->HttpStatus.SERVICE_UNAVAILABLE;};
    return ResponseEntity.status(status).body(new ApiError(e.code(),e.getMessage(),Instant.now()));
  }
  @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class})
  ResponseEntity<ApiError> invalid(Exception e){return ResponseEntity.badRequest().body(new ApiError("INVALID_BATTLE_TAG","Format attendu : Pseudo#1234",Instant.now()));}
}

