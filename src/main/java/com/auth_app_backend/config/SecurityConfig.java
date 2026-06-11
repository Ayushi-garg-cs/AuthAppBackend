package com.auth_app_backend.config;

import com.auth_app_backend.dtos.ServletErrorResponse;
import com.auth_app_backend.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement((SessionManagementConfigurer<HttpSecurity> sm) -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                //jwt auth use kr rhe h to httpbasic vaali line hta denge
                //.httpBasic(Customizer.withDefaults());
                //when someone is trying to access protected apis
                .exceptionHandling((ExceptionHandlingConfigurer<HttpSecurity>  ex) ->ex.authenticationEntryPoint((request, response, authException) -> {
                    //error message
                    authException.printStackTrace();
                    response.setStatus(401);
                    response.setContentType("application/json");
                    String message="Unauthorized access!"+authException.getMessage();

                    String error=(String) request.getAttribute("error");
                    if(error!=null){
                        message=error;
                    }

                    Map<String, Object> errorMap = Map.of(
                            "status", 401,
                            "error", "Unauthorized Access",
                            "message", message,
                            "path", request.getRequestURI()
                    );
                    //using object mapper write upper vala Map in form of String
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(errorMap));
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
    //this bean will used to fetch users
    //we have overwrite user and password jo log m aata tha ab passowrd log m ayega bhi nhi and usse api access bhi nhi hogi postman m
    //isme humne jo "User" use kiya h vo spring security se provided h but we want our own user entity class jo login kr rha hoga
    //when u'll open spring security User class it was implementing UserDetails class so why not we implement UserDetails in our own User entity
//    @Bean
//    public UserDetailsService user(){
//        User.UserBuilder userBuilder= User.withDefaultPasswordEncoder();
//        UserDetails user1=userBuilder.username("Ayushi").password("Ayushi@123").roles("ADMIN").build();
//        UserDetails user2=userBuilder.username("Uday").password("Uday@123").roles("ADMIN").build();
//        UserDetails user3=userBuilder.username("Sana").password("Sana@123").roles("USER").build();
//        return  new InMemoryUserDetailsManager(user1,user2,user3);
//    }

    //User implements UserDetails//our own User entity

}
