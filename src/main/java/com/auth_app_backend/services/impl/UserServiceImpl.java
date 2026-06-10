package com.auth_app_backend.services.impl;

import com.auth_app_backend.dtos.UserDto;
import com.auth_app_backend.entities.Provider;
import com.auth_app_backend.entities.User;
import com.auth_app_backend.exceptions.ResourceNotFoundException;
import com.auth_app_backend.helpers.UserHelper;
import com.auth_app_backend.repositories.UserRepository;
import com.auth_app_backend.services.UserService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        System.out.println("DTO Password = " + userDto.getPassword());
        if(userDto.getEmail() == null || userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }

        User user=modelMapper.map(userDto, User.class);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setProvider(userDto.getProvider()!=null?userDto.getProvider(): Provider.LOCAL);
        //role assigned to user...for Authorization
        //TODO
        User user1=userRepository.save(user);
        return modelMapper.map(user1,UserDto.class);
    }

    //simple resourcenotfound use krne se log m to "User with given email do not exist" ye likha aarha h but postman pr 5000internal error aarha h but hum chahte h user ko bhi msg seen ho
    //thats whywe will use globalexception
    @Override
    public UserDto getUserByEmail(String email) {
        User user= userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with given email do not exist"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID id=UUID.fromString(userId);
        User existingUser=userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User with given id do not exist"));
        //we are not going to change email id
        if(userDto.getName() !=null) existingUser.setName(userDto.getName());
        if(userDto.getImage() !=null) existingUser.setImage(userDto.getImage());
        if(userDto.getProvider() !=null) existingUser.setProvider(userDto.getProvider());
        //TODO:change password updation logic
        if(userDto.getPassword() !=null) existingUser.setPassword(userDto.getPassword());
        existingUser.setEnable(userDto.isEnable());
        User updateduser=userRepository.save(existingUser);
        return modelMapper.map(updateduser,UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID id=UserHelper.parseUUID(userId);
        User user=userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User with this id do not exist"));
        userRepository.delete(user);
    }

    @Override
    public UserDto getUserById(String userId) {
        UUID id=UserHelper.parseUUID(userId);
        User user=userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User with this id do not exist"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(
                        user->modelMapper.map(user,UserDto.class)
                )
                .toList();
    }
}
