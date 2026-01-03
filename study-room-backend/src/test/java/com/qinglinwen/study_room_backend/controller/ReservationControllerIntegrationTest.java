package com.qinglinwen.study_room_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglinwen.study_room_backend.dto.LoginRequest;
import com.qinglinwen.study_room_backend.dto.RegisterRequest;
import com.qinglinwen.study_room_backend.dto.ReservationRequest;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudyRoomRepository studyRoomRepository;

    @Autowired
    private SeatRepository seatRepository;

    private Long roomId;
    private Long seat1Id;
    private Long seat2Id;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        studyRoomRepository.deleteAll();

        StudyRoom room = new StudyRoom();
        room.setName("Room A");
        room.setLocation("Floor 1");
        room.setDescription("Test Room");
        room.setStatus(1);
        room = studyRoomRepository.save(room);
        roomId = room.getId();

        Seat seat1 = new Seat();
        seat1.setRoomId(roomId);
        seat1.setSeatCode("A1");
        seat1.setX(1);
        seat1.setY(1);
        seat1.setStatus(1);
        seat1 = seatRepository.save(seat1);
        seat1Id = seat1.getId();

        Seat seat2 = new Seat();
        seat2.setRoomId(roomId);
        seat2.setSeatCode("A2");
        seat2.setX(2);
        seat2.setY(1);
        seat2.setStatus(1);
        seat2 = seatRepository.save(seat2);
        seat2Id = seat2.getId();
    }

    @Test
    void reservationFlowUsesRealUserIdHeader() throws Exception {
        Long user1Id = registerAndLogin("alice", "alice@example.com");
        Long user2Id = registerAndLogin("bob", "bob@example.com");

        Long reservationId = createReservation(user2Id, seat2Id);

        mockMvc.perform(get("/api/reservations/my")
                        .header("X-User-Id", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/reservations/my")
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservationId.intValue()))
                .andExpect(jsonPath("$[0].seatId").value(seat2Id.intValue()));

        mockMvc.perform(put("/api/reservations/{id}", reservationId)
                        .header("X-User-Id", user1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReservationRequest(seat1Id))))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("You do not have permission to update this reservation"));

        mockMvc.perform(put("/api/reservations/{id}", reservationId)
                        .header("X-User-Id", user2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReservationRequest(seat1Id))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation updated successfully"));

        mockMvc.perform(delete("/api/reservations/{id}", reservationId)
                        .header("X-User-Id", user1Id))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("You do not have permission to cancel this reservation"));

        mockMvc.perform(delete("/api/reservations/{id}", reservationId)
                        .header("X-User-Id", user2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation cancelled successfully"));
    }

    @Test
    void reservationEndpointsRequireUserIdHeader() throws Exception {
        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));

        mockMvc.perform(get("/api/reservations/my")
                        .header("X-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid X-User-Id format"));
    }

    @Test
    void timelineEndpointsReturnHourlyAvailability() throws Exception {
        Long userId = registerAndLogin("charlie", "charlie@example.com");
        createReservation(userId, seat1Id);

        String reserveDate = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/rooms/{roomId}/timeline", roomId)
                        .param("date", reserveDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[1].hour").value(9))
                .andExpect(jsonPath("$[1].occupied").value(1))
                .andExpect(jsonPath("$[1].available").value(1))
                .andExpect(jsonPath("$[1].status").value("partial"))
                .andExpect(jsonPath("$[3].hour").value(11))
                .andExpect(jsonPath("$[3].occupied").value(0));

        mockMvc.perform(get("/api/seats/{seatId}/timeline", seat1Id)
                        .param("date", reserveDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[1].hour").value(9))
                .andExpect(jsonPath("$[1].reserved").value(true))
                .andExpect(jsonPath("$[1].available").value(false))
                .andExpect(jsonPath("$[3].hour").value(11))
                .andExpect(jsonPath("$[3].reserved").value(false));
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
        assertThat(id).isNotNull();
        return id.longValue();
    }

    private Long createReservation(Long userId, Long seatId) throws Exception {
        String response = mockMvc.perform(post("/api/reservations")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildReservationRequest(seatId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Number reservationId = (Number) body.get("reservationId");
        return reservationId.longValue();
    }

    private ReservationRequest buildReservationRequest(Long seatId) {
        ReservationRequest request = new ReservationRequest();
        request.setRoomId(roomId);
        request.setSeatId(seatId);
        request.setReserveDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(11, 0));
        return request;
    }
}
