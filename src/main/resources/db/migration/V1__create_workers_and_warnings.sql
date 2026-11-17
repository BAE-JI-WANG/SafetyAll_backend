-- 작업자(현재 근무 상태). 기기(device_id)당 한 행이며, 종료하면 ended_at을 채우고 다시 시작하면 재사용한다.
CREATE TABLE workers (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    device_id        VARCHAR(64) NOT NULL,
    worker_name      VARCHAR(50) NOT NULL,
    worker_phone_num VARCHAR(30) NOT NULL,
    worker_group     VARCHAR(50) NOT NULL,
    status           VARCHAR(30) NOT NULL,
    latitude         DOUBLE      NOT NULL,
    longitude        DOUBLE      NOT NULL,
    started_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    ended_at         DATETIME(6) NULL,
    CONSTRAINT pk_workers PRIMARY KEY (id),
    CONSTRAINT uk_workers_device_id UNIQUE (device_id)
);

CREATE INDEX idx_workers_ended_at ON workers (ended_at);

-- 경고 이력. 작업자가 종료되어도 남도록 작업자 정보를 그대로 복사해 둔다.
-- idx는 대시보드의 "번호"로 쓰이며, resolved_at이 NULL이면 아직 처리되지 않은 경고다.
CREATE TABLE worker_warnings (
    idx              BIGINT       NOT NULL AUTO_INCREMENT,
    device_id        VARCHAR(64)  NOT NULL,
    worker_name      VARCHAR(50)  NOT NULL,
    worker_phone_num VARCHAR(30)  NOT NULL,
    worker_group     VARCHAR(50)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    reason           VARCHAR(100) NOT NULL,
    result           VARCHAR(500) NULL,
    latitude         DOUBLE       NOT NULL,
    longitude        DOUBLE       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    resolved_at      DATETIME(6)  NULL,
    CONSTRAINT pk_worker_warnings PRIMARY KEY (idx)
);

CREATE INDEX idx_worker_warnings_resolved_at ON worker_warnings (resolved_at);
CREATE INDEX idx_worker_warnings_device_reason ON worker_warnings (device_id, reason, created_at);
