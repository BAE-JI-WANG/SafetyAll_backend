package com.safetyall.backend;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.safetyall.backend.warning.WorkerWarningRepository;

@SpringBootTest(properties = {"safetyall.history-limit=2", "safetyall.warning-dedupe-window=PT0S"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoryLimitTest {

    @Autowired MockMvc mvc;
    @Autowired WorkerWarningRepository warnings;

    @BeforeEach
    void reset() {
        warnings.deleteAll();
    }

    @Test
    void historyReturnsNewestFirstUpToTheLimit() throws Exception {
        for (String reason : new String[]{"첫째", "둘째", "셋째"}) {
            mvc.perform(post("/Api/v1/Api.php").queryParam("apicall", "insertWorkerWarning")
                            .param("deviceId", "dev-1").param("workerName", "홍길동")
                            .param("workerPhoneNum", "010").param("workerGroup", "안전팀")
                            .param("reason", reason))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/Api/v1/Api.php").queryParam("apicall", "selectWorkerWarning"))
                .andExpect(jsonPath("$.workersWarning", hasSize(2)))
                .andExpect(jsonPath("$.workersWarning[*].reason", contains("셋째", "둘째")))
                // 아직 처리되지 않은 경고는 제한 없이 모두 돌려준다
                .andExpect(jsonPath("$.workers", hasSize(3)));
    }
}
