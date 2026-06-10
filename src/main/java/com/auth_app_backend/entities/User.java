package com.auth_app_backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name="users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;
    @Column(name = "user_email", unique = true,length = 100)
    private String email;
    @Column(name="user_name",length = 100)
    private String name;
    private String password;
    private String image;
    private boolean enable=true;

    //These are automatically handled by jpa..
    private Instant createdAt=Instant.now();
    private Instant updatedAt=Instant.now();

    //extra info baadme add krlena
    //private String gender;
    //private Address address;

    //Provider->jinse aap chahte ho user login kr skta ho
    //Do not import Provider...let create it as a enum
    //we are giving provider=local(default value in case user do not give)
    @Enumerated(EnumType.STRING)
    private Provider provider=Provider.LOCAL;//Provider is an enum


    //do not import role..create it as a class
    //EAGER means jis time hum user ko database se fetch kre uss time role bhi saath m aye
    //a user can have many roles at a time(e.g.,ADMIN+GUEST)
    //one role can be assigned to many users
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles=new HashSet<>();

    //this will start when db m table bnn jayegi to ye chalega
    @PrePersist
    protected void onCreate() {
        Instant now=Instant.now();
        if(createdAt==null) createdAt=now;
        updatedAt=now;
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt=Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles
                .stream()
                .map(role->new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    //hum email se login krenge..so spring security ke liye humara email uska username h
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        return this.enable;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
