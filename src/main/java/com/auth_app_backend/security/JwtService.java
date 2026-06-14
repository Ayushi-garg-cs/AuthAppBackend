package com.auth_app_backend.security;

import com.auth_app_backend.entities.Role;
import com.auth_app_backend.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
                      @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {

        if(secret == null || secret.length()<64){
            throw new IllegalArgumentException("Secret is required");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;

    }

    //generate access token
    public String generateAccessToken(User user){
        Instant now = Instant.now();
        List<String> roles=user.getRoles()==null?List.of():user.getRoles().stream().map(Role::getName).toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email",user.getEmail(),
                        "roles",roles,
                        //type-access token
                        "typ","access"
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

    }

    //generate refresh token
    public String generateRefreshToken(User user, String id){
        Instant now = Instant.now();
        return Jwts.builder()
                .id(id)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ","refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
//hello
    }

    //parse the token
    public Jws<Claims> parse(String token){
        try{
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        }catch(JwtException e){
            throw e;
        }
    }

    public boolean isAccessToken(String token){
        Claims c=parse(token).getPayload();//getbody is deprecated version
        return "access".equals(c.get("typ"));
    }

    public boolean isRefreshToken(String token){
        Claims c=parse(token).getPayload();//getbody->getPayload is deprecated version
        return "refresh".equals(c.get("typ"));
    }

    public UUID getUserId(String token){
        Claims c=parse(token).getPayload();
        return  UUID.fromString(c.getSubject());
    }

    //get id of token
    public String getTokenId(String token){
        return parse(token).getPayload().getId();
    }
}
