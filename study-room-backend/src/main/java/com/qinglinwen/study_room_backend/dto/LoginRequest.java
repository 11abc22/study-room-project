package com.qinglinwen.study_room_backend.dto;
import lombok.Data;
@Data
public class LoginRequest {
    private String email;
    private String password;
}