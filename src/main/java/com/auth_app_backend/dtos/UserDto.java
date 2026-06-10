package com.auth_app_backend.dtos;

import com.auth_app_backend.entities.Provider;
import com.auth_app_backend.entities.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String email;
    private String name;
    private String password;
    private String image;
    private boolean enable=true;
    private Instant createdAt=Instant.now();
    private Instant updatedAt=Instant.now();
    private Provider provider=Provider.LOCAL;//Provider is an enum
    private Set<RoleDto> roles=new HashSet<>();
}
