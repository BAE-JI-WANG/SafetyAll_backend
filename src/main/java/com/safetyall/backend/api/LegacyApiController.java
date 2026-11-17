package com.safetyall.backend.api;

import static com.safetyall.backend.api.RequestFields.positiveId;
import static com.safetyall.backend.api.RequestFields.required;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safetyall.backend.common.InvalidRequestException;
import com.safetyall.backend.warning.WarningService;
import com.safetyall.backend.warning.WorkerWarning;
import com.safetyall.backend.worker.Worker;
import com.safetyall.backend.worker.WorkerService;

/**
 * 기존 PHP 서버({@code Api/v1/Api.php?apicall=...})와 같은 주소·파라미터·응답 모양을 지원한다.
 * 안드로이드 앱과 React 대시보드를 코드 수정 없이 이 서버로 돌릴 수 있게 하는 것이 목적이다.
 *
 * 대시보드는 {@code Api.php}를 붙여 호출하기도 하고, 환경 변수 주소 뒤에 바로 {@code ?apicall=}만
 * 붙여 호출하기도 하므로 세 가지 경로를 모두 받는다(Spring 7은 끝 슬래시를 자동으로 맞춰 주지 않는다).
 */
@RestController
@RequestMapping({"/Api/v1/Api.php", "/Api/v1/", "/Api/v1"})
class LegacyApiController {

    private final WorkerService workerService;
    private final WarningService warningService;
    private final ViewMapper views;

    LegacyApiController(WorkerService workerService, WarningService warningService, ViewMapper views) {
        this.workerService = workerService;
        this.warningService = warningService;
        this.views = views;
    }

    // ---- 안드로이드 앱 -------------------------------------------------------------------------

    @PostMapping(params = "apicall=insertWorker")
    ApiBody insertWorker(@ModelAttribute WorkerReportForm form) {
        workerService.start(form.forWorker());
        return ApiBody.ok("Worker started");
    }

    @PostMapping(params = "apicall=updateWorkerStatus")
    ApiBody updateWorkerStatus(@ModelAttribute WorkerReportForm form) {
        workerService.updateStatus(form.forWorker());
        return ApiBody.ok("Worker status updated");
    }

    @PostMapping(params = "apicall=insertWorkerWarning")
    ApiBody insertWorkerWarning(@ModelAttribute WorkerReportForm form) {
        WorkerWarning warning = warningService.record(form.forWarning());
        return ApiBody.ok("Warning recorded").and("idx", warning.getIdx());
    }

    @GetMapping(params = "apicall=deleteWorker")
    ApiBody deleteWorker(@RequestParam(required = false) String deviceId) {
        boolean ended = workerService.end(required("deviceId", deviceId, 64));
        return ApiBody.ok(ended ? "Worker ended" : "Worker already ended");
    }

    // ---- 대시보드 ------------------------------------------------------------------------------

    @GetMapping(params = "apicall=selectWorker")
    ApiBody selectWorker() {
        return ApiBody.ok("Workers retrieved")
                .and("workers", views.workers(workerService.active()));
    }

    /** 대시보드는 개수에 workersCaution, 지도에는 workers를 읽으므로 둘 다 채운다. */
    @GetMapping(params = "apicall=selectWorkerCaution")
    ApiBody selectWorkerCaution() {
        List<WorkerView> caution = views.workers(workerService.caution());
        return ApiBody.ok("Caution workers retrieved")
                .and("workersCaution", caution)
                .and("workers", caution);
    }

    /** 이력 화면은 workersWarning, 지도는 workers를 읽는다. 지도에는 아직 처리되지 않은 경고만 찍는다. */
    @GetMapping(params = "apicall=selectWorkerWarning")
    ApiBody selectWorkerWarning() {
        List<WorkerWarning> unresolved = warningService.unresolved();
        return ApiBody.ok("Warning history retrieved")
                .and("workersWarning", views.warnings(warningService.history()))
                .and("workers", views.warningPositions(unresolved));
    }

    /** 처리되지 않은 경고. 개수가 "경고" 카드와 알림 팝업을 움직인다. */
    @GetMapping(params = "apicall=selectDReadWarning")
    ApiBody selectDReadWarning() {
        return ApiBody.ok("Unresolved warnings retrieved")
                .and("DReadWarning", views.warnings(warningService.unresolved()));
    }

    /** 대시보드는 이 호출을 multipart FormData(idx, result)로 보낸다. */
    @PostMapping(params = "apicall=updateWarningResult")
    ApiBody updateWarningResult(@RequestParam(required = false) String idx,
                                @RequestParam(required = false) String result) {
        WorkerWarning warning = warningService.resolve(
                positiveId("idx", idx), required("result", result, 500));
        return ApiBody.ok("Warning resolved").and("idx", warning.getIdx());
    }

    // ---- 그 외 ---------------------------------------------------------------------------------

    /** apicall이 없거나 모르는 값이거나 메서드가 맞지 않는 모든 요청(CORS 사전 요청 포함)을 받는다. */
    @RequestMapping
    ApiBody unsupported(@RequestParam(required = false) String apicall) {
        if (apicall == null || apicall.isBlank()) {
            throw new InvalidRequestException("apicall is required");
        }
        throw new InvalidRequestException("Unsupported apicall for this HTTP method");
    }
}
