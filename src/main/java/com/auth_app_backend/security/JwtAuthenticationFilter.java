package com.auth_app_backend.security;

import com.auth_app_backend.repositories.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService  jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //fetch the token first
        String header=request.getHeader("Authorization");
        if (header != null || header.startsWith("Bearer ")) {
            //extract the token and validate and then create authentication and then security context ke andar set krna
            String token=header.substring(7);

            //for access token
            try{

                //last m for more enhance
                if(!jwtService.isAccessToken(token)){
                    filterChain.doFilter(request,response);
                    return;
                }

                //verify the token
                Jws<Claims> parse=jwtService.parse(token);
                Claims payload=parse.getPayload();//getBody is deprecated
                String userId=payload.getSubject();
                UUID userUuid=UUID.fromString(userId);
                //or
                //UUID userUuid=UserHelper.parseUUID(userId)
                userRepository.findById(userUuid)
                        .ifPresent(user -> {

                            //last m kro ye..check if user is enable or not
                            if(!user.isEnable()){
                                try {
                                    filterChain.doFilter(request,response);
                                } catch (IOException | ServletException e) {
                                    throw new RuntimeException(e);
                                }
                                return;
                            }
                            //user mil gaya dB se
                            List<GrantedAuthority> authorities = user.getRoles()==null?List.of():user.getRoles()
                                    .stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
                            //or
                            //Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
                            UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),null,authorities
                            );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            //set authentication to securityContext
                            //SecurityContextHolder.getContext().setAuthentication(authentication);
                            //last m for more enhance
                            if(SecurityContextHolder.getContext().getAuthentication()!=null){
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                });

            }catch(ExpiredJwtException e){
                e.printStackTrace();
            }catch(MalformedJwtException e){
                e.printStackTrace();
            }catch(JwtException e){
                e.printStackTrace();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        //token null ho ya na ho ..aage forward to krenge request
        filterChain.doFilter(request, response);
    }
}
