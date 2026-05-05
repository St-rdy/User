package com.stardy.user;

import com.stardy.user.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class UserApplicationTests {
    // 애플리케이션 컨텍스트가 정상적으로 로드되는지만 확인하는 테스트
    // Bean 설정 오류, 의존성 누락 등을 가장 먼저 잡아낼 수 있음
    @Test
    void contextLoads() {
    }
}
