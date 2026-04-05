package com.qinglinwen.study_room_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglinwen.study_room_backend.dto.LoginRequest;
import com.qinglinwen.study_room_backend.dto.RegisterRequest;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.repository.SeatCommentRepository;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeatCommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatCommentRepository seatCommentRepository;

    private Long seatId;

    @BeforeEach
    void setUp() {
        seatCommentRepository.deleteAll();
        seatRepository.deleteAll();
        studyRoomRepository.deleteAll();

        StudyRoom room = new StudyRoom();
        room.setName("Room A");
        room.setLocation("Floor 1");
        room.setDescription("Test Room");
        room.setStatus(1);
        room = studyRoomRepository.save(room);

        Seat seat = new Seat();
        seat.setRoomId(room.getId());
        seat.setSeatCode("A1");
        seat.setX(1);
        seat.setY(1);
        seat.setStatus(0);
        seat = seatRepository.save(seat);
        seatId = seat.getId();
    }

    @Test
    void canCommentOnReservedOrUnavailableSeatAndKeepLatestTen() throws Exception {
        Long userId = registerAndLogin("commenter", "commenter@example.com");

        for (int i = 1; i <= 11; i++) {
            mockMvc.perform(post("/api/seats/{seatId}/comments", seatId)
                            .header("X-User-Id", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Comment " + i + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Comment " + i));
        }

        mockMvc.perform(get("/api/seats/{seatId}/comments", seatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].content").value("Comment 11"))
                .andExpect(jsonPath("$[9].content").value("Comment 2"));
    }

    @Test
    void createCommentValidatesHeaderAndContentLength() throws Exception {
        Long userId = registerAndLogin("validator", "validator@example.com");

        mockMvc.perform(post("/api/seats/{seatId}/comments", seatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));

        mockMvc.perform(post("/api/seats/{seatId}/comments", seatId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Comment content cannot be empty"));

        String longContent = "123456789012345678901234567890123456789012345678901";
        mockMvc.perform(post("/api/seats/{seatId}/comments", seatId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + longContent + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each comment must be 50 characters or fewer"));
    }

    private Long registerAndLogin(String username, String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").exists())
                .andExpect(jsonPath("$.user.username").value(username))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> response = objectMapper.readValue(loginResponse, Map.class);
        Map<?, ?> user = (Map<?, ?>) response.get("user");
        Number id = (Number) user.get("id");
        return id.longValue();
    }
}
