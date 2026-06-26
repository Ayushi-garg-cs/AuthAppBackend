package com.auth_app_backend.controllers;

import com.auth_app_backend.dtos.LoginRequest;
import com.auth_app_backend.dtos.RefreshTokenRequest;
import com.auth_app_backend.dtos.TokenResponse;
import com.auth_app_backend.dtos.UserDto;
import com.auth_app_backend.entities.RefreshToken;
import com.auth_app_backend.entities.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.security.CookieService;
import com.auth_app_backend.security.JwtService;
import com.auth_app_backend.services.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.dnd.DragSourceMotionListener;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
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
    private final CookieService cookieService;


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response){
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

        //use cookie service to attach refresh token in cookie
        cookieService.attachRefreshCookie(response,refreshToken,(int)jwtService.getRefreshTtlSeconds());
        cookieService.noStoreHeaders(response);

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

    //api to renew access and refresh token
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse>refreshToken(@RequestBody(required = false) RefreshTokenRequest body, HttpServletResponse response, HttpServletRequest request) {
        String refreshToken=readRefreshTokenFromRequest(body,request)
                .orElseThrow(()->new BadCredentialsException("Refresh token is missing"));

        if(!jwtService.isRefreshToken(refreshToken)){
            throw new BadCredentialsException("Invalid refresh token type");
        }

        String jti=jwtService.getTokenId(refreshToken);
        UUID userId=jwtService.getUserId(refreshToken);
        RefreshToken storedRefreshToken=refreshTokenRepository
                .findByJti(jti).orElseThrow(()->new  BadCredentialsException("Invalid refresh token"));
        //extra
        if(storedRefreshToken.isRevoked()){
            throw new BadCredentialsException("Refresh token expired or revoked");
        }
        if(storedRefreshToken.getExpiresAt().isBefore(Instant.now())){
            throw new BadCredentialsException("Refresh token expired");
        }
        if(!storedRefreshToken.getUser().getId().equals(userId)){
            throw new BadCredentialsException("Refresh token does not not belong to this user");
        }

        //refresh token ko rotate
        storedRefreshToken.setRevoked(true);
        String newJti=UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);

        User user=storedRefreshToken.getUser();
        var newRefreshTokenOb=RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenOb);
        String newAccessToken=jwtService.generateAccessToken(user);
        String newRefreshToken=jwtService.generateRefreshToken(user,newRefreshTokenOb.getJti());

        cookieService.attachRefreshCookie(response,newRefreshToken,(int)jwtService.getRefreshTtlSeconds());
        cookieService.noStoreHeaders(response);
        return ResponseEntity.ok(TokenResponse.bearer(newAccessToken,newRefreshToken,jwtService.getAccessTtlSeconds(),modelMapper.map(user,UserDto.class)));

    }

    //this method will read refresh token from request header or body
    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        //prefer reading the cookie first and then get refresh token
        if(request.getCookies()!=null){
            Optional<String> fromCookie=Arrays.stream(request.getCookies())
                    .filter(c->cookieService.getRefreshTokenCookieName().equals(c.getName()))
                    .map(Cookie::getValue)
                    .filter(cookie->!cookie.isBlank())
                    .findFirst();

            if(fromCookie.isPresent()){
                return fromCookie;
            }
        }
        //nhi to direct refreshToken ki body dekho
        if(body!=null && body.refreshToken()!=null){
            return Optional.of(body.refreshToken());
        }
        return Optional.empty();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response) {
        readRefreshTokenFromRequest(null,request).ifPresent(token->{
            try{
                if(jwtService.isRefreshToken(token)){
                    String jti=jwtService.getTokenId(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt->{
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }

            }catch(JwtException ignored){}
        });

        //use Cookie util
        cookieService.clearRefreshCookie(response);
        cookieService.noStoreHeaders(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerUser(userDto));
    }
}
