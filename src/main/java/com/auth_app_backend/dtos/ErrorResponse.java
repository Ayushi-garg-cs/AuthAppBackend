package com.auth_app_backend.dtos;


import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;


@Getter
@Setter
public class ErrorResponse {
    String message;
    HttpStatus status;

    public ErrorResponse(String message, HttpStatus httpStatus) {
        this.message = message;
        this.status = httpStatus;
    }
}
