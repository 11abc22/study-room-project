package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.dto.SeatCommentCreateRequest;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.SeatComment;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.SeatCommentRepository;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import com.qinglinwen.study_room_backend.vo.SeatCommentVO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SeatCommentService {

    private static final int MAX_COMMENT_LENGTH = 50;
    private static final int MAX_COMMENT_COUNT = 10;

    private final SeatCommentRepository seatCommentRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public SeatCommentService(SeatCommentRepository seatCommentRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository) {
        this.seatCommentRepository = seatCommentRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    public List<SeatCommentVO> listComments(Long seatId) {
        ensureSeatExists(seatId);

        List<SeatComment> comments = seatCommentRepository.findTop10BySeatIdOrderByCreatedAtDescIdDesc(seatId);
        Map<Long, User> userMap = userRepository.findAllById(comments.stream()
                        .map(SeatComment::getUserId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .map(comment -> toVO(comment, userMap.get(comment.getUserId())))
                .toList();
    }

    @Transactional
    public SeatCommentVO createComment(Long seatId, Long userId, SeatCommentCreateRequest request) {
        ensureSeatExists(seatId);
        validateRequest(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        SeatComment comment = new SeatComment();
        comment.setSeatId(seatId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        SeatComment saved = seatCommentRepository.save(comment);

        trimOverflowComments(seatId);
        return toVO(saved, user);
    }

    private void ensureSeatExists(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Seat not found"));

        if (seat.getId() == null) {
            throw new ResponseStatusException(NOT_FOUND, "Seat not found");
        }
    }

    private void validateRequest(SeatCommentCreateRequest request) {
        if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Comment content cannot be empty");
        }

        String content = request.getContent().trim();
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Each comment must be 50 characters or fewer");
        }
    }

    private void trimOverflowComments(Long seatId) {
        long count = seatCommentRepository.countBySeatId(seatId);
        if (count <= MAX_COMMENT_COUNT) {
            return;
        }

        int overflow = (int) (count - MAX_COMMENT_COUNT);
        List<SeatComment> oldestComments = seatCommentRepository.findBySeatIdOrderByCreatedAtAscIdAsc(seatId)
                .stream()
                .limit(overflow)
                .toList();
        seatCommentRepository.deleteAll(oldestComments);
    }

    private SeatCommentVO toVO(SeatComment comment, User user) {
        SeatCommentVO vo = new SeatCommentVO();
        vo.setId(comment.getId());
        vo.setSeatId(comment.getSeatId());
        vo.setUserId(comment.getUserId());
        vo.setUsername(user != null ? user.getUsername() : "User " + comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setSeatCode(getSeatCode(comment.getSeatId()));
        return vo;
    }

    private String getSeatCode(Long seatId) {
        return seatRepository.findById(seatId)
                .map(Seat::getSeatCode)
                .orElse("Unknown seat");
    }
}
