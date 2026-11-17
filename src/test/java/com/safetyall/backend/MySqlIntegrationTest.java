package com.safetyall.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.safetyall.backend.TestClockConfig.MutableClock;

/**
 * 운영과 같은 MySQL 8.4에서 H2로는 확인할 수 없는 것을 검증한다.
 * Flyway 마이그레이션, 스키마 검증(ddl-auto=validate), utf8mb4 저장, UTC 저장/표시 시간대.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class MySqlIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

    private static TimeZone originalTimeZone;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> MYSQL.getJdbcUrl() + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeAll
    static void serverRunsInSeoul() {
        // 운영 서버가 한국 시간대여도 DB에는 UTC로 저장되는지 보기 위해 JVM 시간대를 바꾼다
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    @AfterAll
    static void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;

    @BeforeEach
    void reset() {
        jdbc.update("delete from worker_warnings");
        jdbc.update("delete from workers");
        clock.set(java.time.Instant.parse("2025-01-01T00:00:00Z"));
    }

    @Test
    void koreanAndEmojiAreStoredAsUtf8mb4AndOverlongNamesAreTruncatedByCharacters() throws Exception {
        String emojiName = "😀".repeat(60);
        mvc.perform(post("/Api/v1/Api.php").queryParam("apicall", "insertWorker")
                        .param("deviceId", "dev-1").param("workerName", emojiName)
                        .param("workerPhoneNum", "010-1234-5678").param("workerGroup", "안전팀")
                        .param("status", "작업시작").param("latitude", "37.5").param("longitude", "127.0"))
                .andExpect(status().isOk());

        // VARCHAR(50)은 글자 수 기준이라 이모지 50개(UTF-16으로는 100칸)가 그대로 들어간다
        assertThat(jdbc.queryForObject("select char_length(worker_name) from workers", Integer.class))
                .isEqualTo(50);
        mvc.perform(get("/Api/v1/Api.php").queryParam("apicall", "selectWorker"))
                .andExpect(jsonPath("$.workers", hasSize(1)))
                .andExpect(jsonPath("$.workers[0].workerName").value("😀".repeat(50)))
                .andExpect(jsonPath("$.workers[0].workerGroup").value("안전팀"));
    }

    @Test
    void timestampsAreStoredInUtcAndShownInConfiguredTimezone() throws Exception {
        mvc.perform(post("/Api/v1/Api.php").queryParam("apicall", "insertWorkerWarning")
                        .param("deviceId", "dev-1").param("workerName", "홍길동")
                        .param("workerPhoneNum", "010").param("workerGroup", "안전팀")
                        .param("reason", "낙상"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select cast(created_at as char) from worker_warnings", String.class))
                .startsWith("2025-01-01 00:00:00");
        mvc.perform(get("/Api/v1/Api.php").queryParam("apicall", "selectDReadWarning"))
                .andExpect(jsonPath("$.DReadWarning[0].dateTime").value("2025-01-01 09:00:00"));
    }
}
