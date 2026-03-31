package com.qinglinwen.study_room_backend.entity;

import jakarta.persistence.*; // Spring Boot 3 使用 jakarta
import lombok.Data;

@Data
@Entity
@Table(name = "users") // 指定数据库中的表名为 'users'
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 省略了 created_at 等其他字段，可以后续添加
}