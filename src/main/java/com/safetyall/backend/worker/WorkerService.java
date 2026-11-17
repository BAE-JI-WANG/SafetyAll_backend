package com.safetyall.backend.worker;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerService {

    private final WorkerRepository workers;
    private final Clock clock;

    public WorkerService(WorkerRepository workers, Clock clock) {
        this.workers = workers;
        this.clock = clock;
    }

    /** 앱에서 헬멧을 연결해 작업을 시작했다(insertWorker). 같은 기기가 이미 있으면 새 작업으로 갱신한다. */
    @Transactional
    public Worker start(WorkerReport report) {
        Instant now = clock.instant();
        Worker worker = workers.findByDeviceId(report.deviceId())
                .orElseGet(() -> new Worker(report.deviceId()));
        worker.begin(report, now);
        return workers.save(worker);
    }

    /**
     * 상태가 바뀌었다(updateWorkerStatus).
     *
     * 서버를 다시 시작했거나 시작 보고가 유실되어 근무 중인 작업자가 없어도, 보고에 작업자 정보가
     * 모두 들어 있으므로 새로 만든다. 위험 상황의 보고를 버리는 것보다 화면에 나타나는 쪽이 안전하다.
     */
    @Transactional
    public Worker updateStatus(WorkerReport report) {
        Instant now = clock.instant();
        Worker worker = workers.findByDeviceId(report.deviceId()).orElse(null);
        if (worker == null) {
            worker = new Worker(report.deviceId());
            worker.begin(report, now);
        } else if (worker.isEnded()) {
            worker.begin(report, now);
        } else {
            worker.update(report, now);
        }
        return workers.save(worker);
    }

    /** 사용자가 연결을 해제했다(deleteWorker). 이미 종료되었거나 없는 기기여도 성공으로 본다(재전송 대비). */
    @Transactional
    public boolean end(String deviceId) {
        return workers.findByDeviceId(deviceId)
                .filter(worker -> !worker.isEnded())
                .map(worker -> {
                    worker.end(clock.instant());
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Worker> active() {
        return workers.findByEndedAtIsNullOrderByIdAsc();
    }

    /** 헬멧을 벗은 작업자(대시보드 "주의"). */
    @Transactional(readOnly = true)
    public List<Worker> caution() {
        return workers.findByEndedAtIsNullAndStatusOrderByIdAsc(WorkerStatus.CAUTION);
    }
}
