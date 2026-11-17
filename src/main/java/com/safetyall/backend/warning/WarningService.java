package com.safetyall.backend.warning;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safetyall.backend.common.Location;
import com.safetyall.backend.common.NotFoundException;
import com.safetyall.backend.config.AppProperties;
import com.safetyall.backend.worker.Worker;
import com.safetyall.backend.worker.WorkerReport;
import com.safetyall.backend.worker.WorkerRepository;

@Service
public class WarningService {

    private final WorkerWarningRepository warnings;
    private final WorkerRepository workers;
    private final AppProperties properties;
    private final Clock clock;

    public WarningService(WorkerWarningRepository warnings, WorkerRepository workers,
                          AppProperties properties, Clock clock) {
        this.warnings = warnings;
        this.workers = workers;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 경고를 기록한다(insertWorkerWarning).
     *
     * 앱은 응답을 못 받으면 같은 요청을 다시 보내므로, 같은 기기에서 같은 사유의 미처리 경고가
     * 설정된 시간(safetyall.warning-dedupe-window) 안에 이미 있으면 새로 만들지 않고 그 경고를 돌려준다.
     */
    @Transactional
    public WorkerWarning record(WorkerReport report) {
        Instant now = clock.instant();
        Instant since = now.minus(properties.warningDedupeWindow());
        List<WorkerWarning> recent = warnings.findRecentUnresolved(
                report.deviceId(), report.reason(), since, PageRequest.of(0, 1));
        if (!recent.isEmpty()) {
            return recent.get(0);
        }
        return warnings.save(new WorkerWarning(
                report.deviceId(), report.workerName(), report.workerPhoneNum(), report.workerGroup(),
                report.status(), report.reason(), locationOf(report), now));
    }

    /** 위치를 못 얻은 보고는 그 작업자가 마지막으로 알려진 위치를 쓴다. */
    private Location locationOf(WorkerReport report) {
        if (!report.location().isUnknown()) {
            return report.location();
        }
        return workers.findByDeviceId(report.deviceId())
                .map(Worker::location)
                .orElse(Location.UNKNOWN);
    }

    /** 담당자가 경고를 처리했다(updateWarningResult). */
    @Transactional
    public WorkerWarning resolve(long idx, String result) {
        WorkerWarning warning = warnings.findById(idx)
                .orElseThrow(() -> new NotFoundException("Warning not found: " + idx));
        warning.resolve(result, clock.instant());
        return warning;
    }

    /** 아직 처리되지 않은 경고(대시보드 "경고" 카드와 팝업). */
    @Transactional(readOnly = true)
    public List<WorkerWarning> unresolved() {
        return warnings.findByResolvedAtIsNullOrderByIdxAsc();
    }

    /** 경고 이력, 최신순. */
    @Transactional(readOnly = true)
    public List<WorkerWarning> history() {
        return warnings.findAllByOrderByIdxDesc(PageRequest.of(0, properties.historyLimit()));
    }
}
