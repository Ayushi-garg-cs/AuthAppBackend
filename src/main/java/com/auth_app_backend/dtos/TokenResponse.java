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

    public static TokenResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserDto user) {

        TokenResponse response = new TokenResponse();
        response.accessToken = accessToken;
        response.refreshToken = refreshToken;
        response.expiresIn = expiresIn;
        response.user = user;

        return response;
    }
}
