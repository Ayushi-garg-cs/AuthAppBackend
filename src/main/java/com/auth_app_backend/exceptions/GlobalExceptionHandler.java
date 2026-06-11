package com.auth_app_backend.exceptions;

import com.auth_app_backend.dtos.ErrorResponse;
import com.auth_app_backend.dtos.ServletErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.function.ServerResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    Logger logger= LoggerFactory.getLogger(this.getClass());

    //servlet se aane vaale error ...JwtException vgrh filter se aayenge
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })
    public ResponseEntity<ServletErrorResponse>  handleAuthException(Exception ex, HttpServletRequest request){
        logger.info("Exception:{}",ex.getClass().getName());
        var servletError=ServletErrorResponse.of(HttpStatus.BAD_REQUEST.value(),"Bad Request",ex.getMessage(),request.getRequestURI());
        return ResponseEntity.badRequest().body(servletError);
    }


    //Resource not found exception::handler
    //u can provide multiple classes in this ExceptionHandler annotation
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> ResourceNotFoundException(ResourceNotFoundException e){
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgumentException(IllegalArgumentException e){
        ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
