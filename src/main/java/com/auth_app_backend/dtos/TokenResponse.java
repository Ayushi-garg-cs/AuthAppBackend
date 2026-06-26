package com.auth_app_backend.dtos;

public record TokenResponse (
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,
    UserDto user){


    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn, UserDto user){
        return new TokenResponse(accessToken,refreshToken,expiresIn,"bearer",user);//token type by default is bearer
    }

}
