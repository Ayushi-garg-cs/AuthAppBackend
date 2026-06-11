package com.auth_app_backend.dtos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ServletErrorResponse(
        int status,
        String error,
        String message,
        String path,
        OffsetDateTime timestamp
) {
    public static ServletErrorResponse of(int status, String error, String message, String path){
        return new ServletErrorResponse(status,error,message,path,OffsetDateTime.now(ZoneOffset.UTC));
    }
}
