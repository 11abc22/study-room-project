package com.qinglinwen.study_room_backend.controller;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateSaveRequest {
    private String subject;
    private String body;
}
