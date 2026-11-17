package com.safetyall.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.safetyall.backend.TestClockConfig.MutableClock;
import com.safetyall.backend.warning.WorkerWarningRepository;
import com.safetyall.backend.worker.WorkerRepository;

/** 안드로이드 앱과 대시보드가 실제로 보내고 읽는 형태를 그대로 따라 한 계약 테스트. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class LegacyApiTest {

    private static final String API = "/Api/v1/Api.php";

    @Autowired MockMvc mvc;
    @Autowired MutableClock clock;
    @Autowired WorkerRepository workers;
    @Autowired WorkerWarningRepository warnings;

    @BeforeEach
    void reset() {
        warnings.deleteAll();
        workers.deleteAll();
        clock.set(java.time.Instant.parse("2025-01-01T00:00:00Z"));
    }

    // ---- 요청 만들기 -----------------------------------------------------------------------------

    /** 앱의 ReportPlanner가 보내는 폼 파라미터와 같은 모양. */
    private static MockHttpServletRequestBuilder report(String call, String deviceId, String status,
                                                        String lat, String lon) {
        return post(API).queryParam("apicall", call)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("deviceId", deviceId)
                .param("latitude", lat)
                .param("longitude", lon)
                .param("workerName", "홍길동")
                .param("workerPhoneNum", "010-1234-5678")
                .param("workerGroup", "안전팀")
                .param("status", status);
    }

    private ResultActions insert(String deviceId, String lat, String lon) throws Exception {
        return mvc.perform(report("insertWorker", deviceId, "작업시작", lat, lon));
    }

    private ResultActions warn(String deviceId, String reason, String lat, String lon) throws Exception {
        return mvc.perform(report("insertWorkerWarning", deviceId, "경고", lat, lon).param("reason", reason));
    }

    private ResultActions call(String apicall) throws Exception {
        return mvc.perform(get(API).queryParam("apicall", apicall));
    }

    // ---- 앱 → 서버 ---------------------------------------------------------------------------------

    @Nested
    class AppFlow {

        @Test
        void startedWorkerShowsUpWithKoreanTextAndKstTime() throws Exception {
            insert("dev-1", "37.5665", "126.9780")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(false));

            call("selectWorker")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(false))
                    .andExpect(jsonPath("$.workers", hasSize(1)))
                    .andExpect(jsonPath("$.workers[0].workerName").value("홍길동"))
                    .andExpect(jsonPath("$.workers[0].workerGroup").value("안전팀"))
                    .andExpect(jsonPath("$.workers[0].workerPhoneNum").value("010-1234-5678"))
                    .andExpect(jsonPath("$.workers[0].status").value("작업시작"))
                    .andExpect(jsonPath("$.workers[0].latitude").value(37.5665))
                    .andExpect(jsonPath("$.workers[0].longitude").value(126.9780))
                    // 2025-01-01T00:00:00Z는 서울 시간으로 09시다
                    .andExpect(jsonPath("$.workers[0].dateTime").value("2025-01-01 09:00:00"))
                    .andExpect(jsonPath("$.workers[0].deviceId").doesNotExist());
        }

        @Test
        void startingTheSameDeviceAgainKeepsOneWorker() throws Exception {
            insert("dev-1", "37.5", "127.0").andExpect(status().isOk());
            insert("dev-1", "37.6", "127.1").andExpect(status().isOk());

            call("selectWorker")
                    .andExpect(jsonPath("$.workers", hasSize(1)))
                    .andExpect(jsonPath("$.workers[0].latitude").value(37.6));
        }

        @Test
        void unwornHelmetCountsAsCautionUnderBothKeys() throws Exception {
            insert("dev-1", "37.5", "127.0");
            insert("dev-2", "37.6", "127.1");
            mvc.perform(report("updateWorkerStatus", "dev-2", "미착용", "37.6", "127.1"))
                    .andExpect(status().isOk());

            // 대시보드는 개수를 workersCaution에서, 지도는 workers에서 읽는다
            call("selectWorkerCaution")
                    .andExpect(jsonPath("$.workersCaution", hasSize(1)))
                    .andExpect(jsonPath("$.workers", hasSize(1)))
                    .andExpect(jsonPath("$.workersCaution[0].status").value("미착용"));

            mvc.perform(report("updateWorkerStatus", "dev-2", "착용", "37.6", "127.1"));
            call("selectWorkerCaution").andExpect(jsonPath("$.workersCaution", hasSize(0)));
        }

        @Test
        void statusUpdateWithoutPriorStartStillAppears() throws Exception {
            mvc.perform(report("updateWorkerStatus", "dev-9", "착용", "37.5", "127.0"))
                    .andExpect(status().isOk());

            call("selectWorker").andExpect(jsonPath("$.workers", hasSize(1)));
        }

        @Test
        void disconnectRemovesWorkerAndIsIdempotent() throws Exception {
            insert("dev-1", "37.5", "127.0");

            mvc.perform(get(API).queryParam("apicall", "deleteWorker").queryParam("deviceId", "dev-1"))
                    .andExpect(status().isOk());
            call("selectWorker").andExpect(jsonPath("$.workers", hasSize(0)));

            // 응답을 못 받은 앱의 재전송과 모르는 기기 모두 성공으로 처리한다
            mvc.perform(get(API).queryParam("apicall", "deleteWorker").queryParam("deviceId", "dev-1"))
                    .andExpect(status().isOk());
            mvc.perform(get(API).queryParam("apicall", "deleteWorker").queryParam("deviceId", "nobody"))
                    .andExpect(status().isOk());

            // 종료된 뒤에 온 상태 보고는 다시 근무 중으로 되살린다
            mvc.perform(report("updateWorkerStatus", "dev-1", "착용", "37.5", "127.0"));
            call("selectWorker").andExpect(jsonPath("$.workers", hasSize(1)));
        }

        @Test
        void deleteWorkerRequiresDeviceId() throws Exception {
            mvc.perform(get(API).queryParam("apicall", "deleteWorker"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(true));
        }
    }

    // ---- 경고 ---------------------------------------------------------------------------------------

    @Nested
    class Warnings {

        @Test
        void warningFlowFromReportToOperatorResolution() throws Exception {
            insert("dev-1", "37.5", "127.0");
            warn("dev-1", "낙상", "37.5", "127.0")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idx").isNumber());

            call("selectDReadWarning")
                    .andExpect(jsonPath("$.DReadWarning", hasSize(1)))
                    .andExpect(jsonPath("$.DReadWarning[0].reason").value("낙상"))
                    .andExpect(jsonPath("$.DReadWarning[0].workerName").value("홍길동"))
                    .andExpect(jsonPath("$.DReadWarning[0].dateTime").value("2025-01-01 09:00:00"));

            // 지도는 workers, 이력은 workersWarning을 읽는다
            call("selectWorkerWarning")
                    .andExpect(jsonPath("$.workersWarning", hasSize(1)))
                    .andExpect(jsonPath("$.workersWarning[0].status").value("경고"))
                    .andExpect(jsonPath("$.workers", hasSize(1)))
                    .andExpect(jsonPath("$.workers[0].latitude").value(37.5));

            String idx = readIdx();
            // 대시보드는 multipart FormData로 보낸다
            mvc.perform(multipart("/Api/v1/?apicall=updateWarningResult")
                            .param("idx", idx).param("result", "현장 확인 완료"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.error").value(false));

            call("selectDReadWarning").andExpect(jsonPath("$.DReadWarning", hasSize(0)));
            call("selectWorkerWarning")
                    .andExpect(jsonPath("$.workersWarning", hasSize(1)))
                    .andExpect(jsonPath("$.workersWarning[0].status").value("처리완료"))
                    .andExpect(jsonPath("$.workersWarning[0].result").value("현장 확인 완료"))
                    .andExpect(jsonPath("$.workers", hasSize(0)));
        }

        private String readIdx() {
            return String.valueOf(warnings.findByResolvedAtIsNullOrderByIdxAsc().get(0).getIdx());
        }

        @Test
        void historyStaysAfterWorkerDisconnects() throws Exception {
            insert("dev-1", "37.5", "127.0");
            warn("dev-1", "어플강제종료", "37.5", "127.0");
            mvc.perform(get(API).queryParam("apicall", "deleteWorker").queryParam("deviceId", "dev-1"));

            call("selectWorker").andExpect(jsonPath("$.workers", hasSize(0)));
            call("selectDReadWarning").andExpect(jsonPath("$.DReadWarning", hasSize(1)));
        }

        @Test
        void warningStatusDefaultsToWarning() throws Exception {
            insert("dev-1", "37.5", "127.0");
            mvc.perform(post(API).queryParam("apicall", "insertWorkerWarning")
                            .param("deviceId", "dev-1").param("workerName", "홍길동")
                            .param("workerPhoneNum", "010").param("workerGroup", "안전팀")
                            .param("reason", "헬멧 연결 끊김"))
                    .andExpect(status().isOk());

            call("selectWorkerWarning").andExpect(jsonPath("$.workersWarning[0].status").value("경고"));
        }

        @Test
        void retransmittedWarningWithinWindowIsNotDuplicated() throws Exception {
            insert("dev-1", "37.5", "127.0");
            warn("dev-1", "낙상", "37.5", "127.0");
            clock.advance(Duration.ofSeconds(5));
            warn("dev-1", "낙상", "37.5", "127.0").andExpect(status().isOk());

            call("selectDReadWarning").andExpect(jsonPath("$.DReadWarning", hasSize(1)));
        }

        @Test
        void sameReasonAfterWindowOrDifferentReasonIsANewWarning() throws Exception {
            insert("dev-1", "37.5", "127.0");
            warn("dev-1", "낙상", "37.5", "127.0");
            clock.advance(Duration.ofSeconds(11));
            warn("dev-1", "낙상", "37.5", "127.0");
            warn("dev-1", "움직임 미감지", "37.5", "127.0");
            warn("dev-2", "낙상", "37.5", "127.0");

            call("selectDReadWarning").andExpect(jsonPath("$.DReadWarning", hasSize(4)));
        }

        @Test
        void resolvedWarningDoesNotSwallowANewOne() throws Exception {
            insert("dev-1", "37.5", "127.0");
            warn("dev-1", "낙상", "37.5", "127.0");
            mvc.perform(post(API).queryParam("apicall", "updateWarningResult")
                    .param("idx", readIdx()).param("result", "처리"));
            warn("dev-1", "낙상", "37.5", "127.0");

            call("selectDReadWarning").andExpect(jsonPath("$.DReadWarning", hasSize(1)));
        }

        @Test
        void unknownLocationKeepsLastKnownPosition() throws Exception {
            insert("dev-1", "37.5", "127.0");
            // GPS를 못 얻은 앱은 0.0, 0.0을 보낸다
            mvc.perform(report("updateWorkerStatus", "dev-1", "착용", "0.0", "0.0"));
            warn("dev-1", "낙상", "0.0", "0.0");

            call("selectWorker")
                    .andExpect(jsonPath("$.workers[0].latitude").value(37.5))
                    .andExpect(jsonPath("$.workers[0].longitude").value(127.0));
            call("selectDReadWarning")
                    .andExpect(jsonPath("$.DReadWarning[0].latitude").value(37.5))
                    .andExpect(jsonPath("$.DReadWarning[0].longitude").value(127.0));
        }

        @Test
        void warningFromUnknownWorkerWithoutLocationIsStillStored() throws Exception {
            warn("ghost", "낙상", "0.0", "0.0").andExpect(status().isOk());

            call("selectDReadWarning")
                    .andExpect(jsonPath("$.DReadWarning", hasSize(1)))
                    .andExpect(jsonPath("$.DReadWarning[0].latitude").value(0.0));
        }

        @Test
        void resolvingUnknownWarningIsNotFound() throws Exception {
            mvc.perform(post(API).queryParam("apicall", "updateWarningResult")
                            .param("idx", "9999").param("result", "처리"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value(true));
        }
    }

    // ---- 검증/오류 ---------------------------------------------------------------------------------

    @Nested
    class Validation {

        @Test
        void missingRequiredParametersAreRejected() throws Exception {
            for (String missing : new String[]{"deviceId", "workerName", "workerPhoneNum", "workerGroup", "status"}) {
                // param()은 값을 추가하므로, 기존 값을 공백으로 교체한다
                MockHttpServletRequestBuilder request = report("insertWorker", "dev-1", "작업시작", "37.5", "127.0")
                        .with(r -> {
                            r.setParameter(missing, "  ");
                            return r;
                        });
                mvc.perform(request)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.error").value(true))
                        .andExpect(jsonPath("$.message").value(missing + " is required"));
            }
        }

        @Test
        void warningRequiresReason() throws Exception {
            mvc.perform(report("insertWorkerWarning", "dev-1", "경고", "37.5", "127.0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("reason is required"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "NaN", "Infinity", "91", "-90.5"})
        void invalidLatitudeIsRejected(String latitude) throws Exception {
            insert("dev-1", latitude, "127.0").andExpect(status().isBadRequest());
        }

        @Test
        void invalidLongitudeIsRejected() throws Exception {
            insert("dev-1", "37.5", "181").andExpect(status().isBadRequest());
        }

        @Test
        void tooLongValuesAreRejected() throws Exception {
            insert("x".repeat(65), "37.5", "127.0")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("deviceId must be at most 64 characters"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "abc", "0", "-3", "1.5"})
        void invalidWarningIdIsRejected(String idx) throws Exception {
            mvc.perform(post(API).queryParam("apicall", "updateWarningResult")
                            .param("idx", idx).param("result", "처리"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void blankResultIsRejected() throws Exception {
            mvc.perform(post(API).queryParam("apicall", "updateWarningResult")
                            .param("idx", "1").param("result", " "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("result is required"));
        }

        @Test
        void unknownOrMissingApicallIsBadRequest() throws Exception {
            call("noSuchCall").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(true));
            mvc.perform(get(API)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("apicall is required"));
        }

        @Test
        void writeCallsOverGetAreRejectedWithoutChangingData() throws Exception {
            mvc.perform(get(API).queryParam("apicall", "insertWorker")
                            .queryParam("deviceId", "dev-1"))
                    .andExpect(status().isBadRequest());

            call("selectWorker").andExpect(jsonPath("$.workers", hasSize(0)));
        }

        @Test
        void unsupportedPathIsNotFound() throws Exception {
            mvc.perform(delete("/Api/v1/other")).andExpect(status().isNotFound());
        }
    }

    // ---- 주소 형태 ---------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"/Api/v1/Api.php", "/Api/v1/", "/Api/v1"})
    void everyUrlFormTheClientsUseIsServed(String path) throws Exception {
        mvc.perform(get(path).queryParam("apicall", "selectWorker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workers", hasSize(0)));
    }

    // ---- CORS --------------------------------------------------------------------------------------

    @Nested
    class Cors {

        @Test
        void configuredDashboardOriginIsAllowed() throws Exception {
            mvc.perform(get(API).queryParam("apicall", "selectWorker").header("Origin", "http://dashboard.test"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://dashboard.test"));
        }

        @Test
        void otherOriginIsRejected() throws Exception {
            mvc.perform(get(API).queryParam("apicall", "selectWorker").header("Origin", "http://evil.test"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void preflightForTheMultipartPostSucceeds() throws Exception {
            mvc.perform(options("/Api/v1/").queryParam("apicall", "updateWarningResult")
                            .header("Origin", "http://dashboard.test")
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://dashboard.test"));
        }
    }
}
