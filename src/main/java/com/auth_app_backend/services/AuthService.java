package com.auth_app_backend.services;

import com.auth_app_backend.dtos.UserDto;

//Auth related things
public interface AuthService {
    //register user
    UserDto registerUser(UserDto userDto);
    //login user
    //UserDto loginUser(UserDto userDto);
}
