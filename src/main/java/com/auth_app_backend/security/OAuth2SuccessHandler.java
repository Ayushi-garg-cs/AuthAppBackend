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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontendSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        log.info("Successful Authentication");
//        response.getWriter().write("Successfully Logged In");
        response.sendRedirect(frontendSuccessUrl);


        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId();

        User user;

        switch (registrationId) {

            case "google" -> {

                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");
                String picture = oAuth2User.getAttribute("picture");

                user = userRepository.findByEmail(email)
                        .orElseGet(() -> {

                            User newUser = User.builder()
                                    .email(email)
                                    .name(name)
                                    .image(picture)
                                    .enable(true)
                                    .provider(Provider.GOOGLE)
                                    .build();

                            log.info("New User Created");

                            return userRepository.save(newUser);
                        });

                log.info("Logged In User : {}", user.getEmail());
            }

            case "github" -> {

                String githubId = oAuth2User.getAttribute("id").toString();
                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");
                String avatar = oAuth2User.getAttribute("avatar_url");

                // Agar user ne GitHub profile me naam nahi diya ho
                if (name == null || name.isBlank()) {
                    name = oAuth2User.getAttribute("login");
                }

                // Agar email private ho
                if (email == null || email.isBlank()) {
                    email = githubId + "@github.local";
                    // Ya yahan exception throw kar sakte ho agar real email mandatory hai
                }

                String finalEmail = email;
                String finalName = name;
                user = userRepository.findByEmail(email)
                        .orElseGet(() -> {

                            User newUser = User.builder()
                                    .email(finalEmail)
                                    .name(finalName)
                                    .image(avatar)
                                    .enable(true)
                                    .provider(Provider.GITHUB)
                                    .build();

                            return userRepository.save(newUser);
                        });

                log.info("Logged In User : {}", user.getEmail());
            }

            default -> throw new RuntimeException("Unsupported OAuth Provider");
        }

        // ===========================
        // Create Refresh Token Entry
        // ===========================

        String jti = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(
                        Instant.now()
                                .plusSeconds(jwtService.getRefreshTtlSeconds())
                )
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        // ===========================
        // Generate JWT Tokens
        // ===========================

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user, jti);

        // ===========================
        // Attach Refresh Token Cookie
        // ===========================

        cookieService.attachRefreshCookie(
                response,
                refreshToken,
                (int) jwtService.getRefreshTtlSeconds()
        );
    }
}