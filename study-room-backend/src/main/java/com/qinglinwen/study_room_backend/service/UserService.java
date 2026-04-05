package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(User user) {
        validateRegistration(user);

        if (userRepository.findByUsername(user.getUsername().trim()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Username already exists");
        }

        if (userRepository.findByEmail(user.getEmail().trim()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already exists");
        }

        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());

        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private void validateRegistration(User user) {
        if (user == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Registration data is required");
        }

        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Username is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Email is required");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}