package com.qinglinwen.study_room_backend.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenderedEmailTemplate {
    private final String subject;
    private final String body;
}
