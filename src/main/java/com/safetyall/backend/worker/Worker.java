package com.safetyall.backend.worker;

import java.time.Instant;

import com.safetyall.backend.common.Location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 현재 근무 중인 작업자. 기기(deviceId)당 한 행이다. */
@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, updatable = false, length = 64)
    private String deviceId;

    @Column(name = "worker_name", nullable = false, length = 50)
    private String workerName = "";

    @Column(name = "worker_phone_num", nullable = false, length = 30)
    private String workerPhoneNum = "";

    @Column(name = "worker_group", nullable = false, length = 50)
    private String workerGroup = "";

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected Worker() {
    }

    public Worker(String deviceId) {
        this.deviceId = deviceId;
    }

    /** 작업을 (다시) 시작한다. */
    public void begin(WorkerReport report, Instant now) {
        apply(report, now);
        this.startedAt = now;
        this.endedAt = null;
    }

    /** 근무 중인 작업자의 상태를 갱신한다. */
    public void update(WorkerReport report, Instant now) {
        apply(report, now);
    }

    public void end(Instant now) {
        this.endedAt = now;
        this.updatedAt = now;
    }

    private void apply(WorkerReport report, Instant now) {
        // 비어 있는 신원과 알 수 없는 위치(0, 0)는 마지막으로 알던 값을 유지한다.
        // 위치를 못 얻은 보고가 지도에서 작업자를 튀게 하거나 이름이 지워지지 않게 하기 위함이다.
        WorkerReport merged = report.filledFrom(this);
        this.workerName = merged.workerName();
        this.workerPhoneNum = merged.workerPhoneNum();
        this.workerGroup = merged.workerGroup();
        this.status = merged.status();
        this.latitude = merged.location().latitude();
        this.longitude = merged.location().longitude();
        this.updatedAt = now;
    }

    public boolean isEnded() {
        return endedAt != null;
    }

    public Location location() {
        return new Location(latitude, longitude);
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getWorkerPhoneNum() {
        return workerPhoneNum;
    }

    public String getWorkerGroup() {
        return workerGroup;
    }

    public String getStatus() {
        return status;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}
