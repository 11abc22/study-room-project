package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.dto.LoginRequest;
import com.qinglinwen.study_room_backend.dto.RegisterRequest;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());

        User registeredUser = userService.register(user);
        registeredUser.setPassword(null);

        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        boolean passwordMatched = userService.checkPassword(
                loginRequest.getPassword(),
                user.getPassword()
        );

        if (!passwordMatched) {
            throw new RuntimeException("密码错误");
        }

        return ResponseEntity.ok(Map.of(
                "message", "登录成功",
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail()
                )
        ));
    }
}
