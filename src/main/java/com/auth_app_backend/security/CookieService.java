package com.auth_app_backend.security;

import com.auth_app_backend.dtos.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Getter
public class CookieService {
    //these all are in application-dev.yml
    private final String refreshTokenCookieName;
    private final boolean cookieSecure;
    private final boolean cookieHttpOnly;
    private final String cookieSameSite;
    private final String cookieDomain;
    private final Logger logger = LoggerFactory.getLogger(CookieService.class);

    public CookieService(
            @Value("${security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${security.jwt.cookie-http-only}")boolean cookieHttpOnly,
            @Value("${security.jwt.cookie-same-site}") String cookieSameSite,
            @Value("${security.jwt.cookie-domain}") String cookieDomain) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSameSite = cookieSameSite;
        this.cookieDomain = cookieDomain;
    }

    //attach secure http only refresh cookie
    //so create method to attach cookie to response
    //value is value of refreshToken
    public void attachRefreshCookie(HttpServletResponse response,String value,int maxAge) {
        logger.info("Attaching cookie with name: {} and value:{}",refreshTokenCookieName,value);
        var responseCookieBuilder=ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite);
        if(cookieDomain!=null && !cookieDomain.isBlank()) {
            responseCookieBuilder.domain(cookieDomain);
        }
        ResponseCookie responseCookie=responseCookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    //clear refresh cookie
    public void clearRefreshCookie(HttpServletResponse response) {
        var builder=ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite);
        if(cookieDomain!=null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie responseCookie=builder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }


    public void noStoreHeaders(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma","no-cache");
    }
}

