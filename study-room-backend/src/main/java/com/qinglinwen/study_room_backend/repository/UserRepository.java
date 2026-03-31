package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository<实体类型, 主键类型>
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA 会根据方法名自动生成查询
    Optional<User> findByEmail(String email);
}