package com.qinglinwen.study_room_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudyRoomBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyRoomBackendApplication.class, args);
	}

}
