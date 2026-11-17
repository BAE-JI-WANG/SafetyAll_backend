package com.safetyall.backend.api;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.safetyall.backend.config.AppProperties;
import com.safetyall.backend.warning.WorkerWarning;
import com.safetyall.backend.worker.Worker;

/** 엔티티를 응답용 뷰로 바꾼다. 시각은 DB의 UTC를 설정된 시간대의 "yyyy-MM-dd HH:mm:ss"로 표시한다. */
@Component
class ViewMapper {

    private final DateTimeFormatter formatter;

    ViewMapper(AppProperties properties) {
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(properties.timezone());
    }

    WorkerView toView(Worker worker) {
        return new WorkerView(
                worker.getWorkerName(), worker.getWorkerGroup(), worker.getWorkerPhoneNum(),
                worker.getStatus(), worker.getLatitude(), worker.getLongitude(),
                format(worker.getUpdatedAt()));
    }

    WarningView toView(WorkerWarning warning) {
        return new WarningView(
                warning.getIdx(), warning.getWorkerName(), warning.getWorkerGroup(),
                warning.getWorkerPhoneNum(), warning.getStatus(), warning.getReason(),
                warning.getResult(), warning.getLatitude(), warning.getLongitude(),
                format(warning.getCreatedAt()));
    }

    List<WorkerView> workers(List<Worker> workers) {
        return workers.stream().map(this::toView).toList();
    }

    List<WarningView> warnings(List<WorkerWarning> warnings) {
        return warnings.stream().map(this::toView).toList();
    }

    /** 경고의 위치만 지도에 찍을 수 있도록 작업자 뷰 모양으로 바꾼다(대시보드의 key mismatch 보정용). */
    List<WorkerView> warningPositions(List<WorkerWarning> warnings) {
        return warnings.stream()
                .map(w -> new WorkerView(
                        w.getWorkerName(), w.getWorkerGroup(), w.getWorkerPhoneNum(),
                        w.getStatus(), w.getLatitude(), w.getLongitude(), format(w.getCreatedAt())))
                .toList();
    }

    private String format(Instant instant) {
        return formatter.format(instant);
    }
}
