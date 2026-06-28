package com.auth_app_backend.security;

import com.auth_app_backend.entities.Provider;
import com.auth_app_backend.entities.RefreshToken;
import com.auth_app_backend.entities.User;
import com.auth_app_backend.repositories.RefreshTokenRepository;
import com.auth_app_backend.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
@AllArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private UserRepository userRepository;
    private JwtService jwtService;
    private RefreshTokenRepository refreshTokenRepository;
    private CookieService cookieService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("Successful Authentication");
        log.info(authentication.toString());

        //principal gives user
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        //identify user
        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }
        log.info("user:" + oAuth2User.getAttributes().toString());
        log.info("registrationId:" + registrationId);

        User user;
        switch (registrationId) {
            case "google":
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();
                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();
                user = User.builder()
                        .email(email)
                        .name(name)
                        .enable(true)
                        .image(picture)
                        .provider(Provider.GOOGLE)
                        .build();
                userRepository.findByEmail(email).ifPresentOrElse(user1 -> {
                    log.info("User is there in databse");
                    log.info(user1.toString());
                }, () -> {
                    userRepository.save(user);
                });
            default:
                throw new RuntimeException("Invalid Registration Id");
        }

        //username
        //user email
        //new usercreate
        //jwt token-->token ke saath front pr redirect
        //ye page pr likha aayega

        //make a refresh token

        String jti=UUID.randomUUID().toString();
        RefreshToken refreshTokendb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build();
        refreshTokenRepository.save(refreshTokendb);
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokendb.getJti());
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSeconds());
        response.getWriter().write("Successfully Logged In");
    }

}
