package com.qinglinwen.study_room_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;   // 新增：导入 ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")  // 新增：让测试加载 src/test/resources/application-test.properties
class StudyRoomBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}