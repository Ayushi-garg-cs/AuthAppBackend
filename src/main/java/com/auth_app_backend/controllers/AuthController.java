package com.auth_app_backend.controllers;

import com.auth_app_backend.dtos.LoginRequest;
import com.auth_app_backend.dtos.TokenResponse;
import com.auth_app_backend.dtos.UserDto;
import com.auth_app_backend.entities.RefreshToken;
import com.auth_app_backend.entities.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    //iss authenticationManager ko bean bhi bnao nhi to problem hogi baadme in securityConfig
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest){
        //authenticate the user
        Authentication authenticate=authenticate(loginRequest);
        User user=userRepository.findByEmail(loginRequest.email()).orElseThrow(()->new BadCredentialsException("Invalid email or password"));
        if(!user.isEnable()){
            throw new DisabledException("User is disabled");
        }

        //generate refresh token
        String jti= UUID.randomUUID().toString();
        var refreshTokenOb= RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        //save refresh token info
        refreshTokenRepository.save(refreshTokenOb);


        //generate access token
        String accessToken=jwtService.generateAccessToken(user);
        //regenerating refresh token using refreshtokenOb that is saved in dB
        String refreshToken=jwtService.generateRefreshToken(user,refreshTokenOb.getJti());
        TokenResponse tokenResponse=TokenResponse.bearer(accessToken,refreshToken,jwtService.getAccessTtlSeconds(),modelMapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);
    }
    private Authentication authenticate(LoginRequest loginRequest){
        try{
            //returns authentication
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password()));
        }catch(Exception e){
            throw new BadCredentialsException("Invalid username or password");
        }
    }


    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}
