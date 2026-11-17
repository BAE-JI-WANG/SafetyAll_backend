package com.safetyall.backend.warning;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkerWarningRepository extends JpaRepository<WorkerWarning, Long> {

    List<WorkerWarning> findByResolvedAtIsNullOrderByIdxAsc();

    List<WorkerWarning> findAllByOrderByIdxDesc(Pageable pageable);

    /** 같은 기기·같은 사유로 최근에 만들어진 미처리 경고(재전송 중복 방지용). */
    @Query("""
            select w from WorkerWarning w
            where w.deviceId = :deviceId and w.reason = :reason
              and w.resolvedAt is null and w.createdAt >= :since
            order by w.idx desc
            """)
    List<WorkerWarning> findRecentUnresolved(@Param("deviceId") String deviceId,
                                             @Param("reason") String reason,
                                             @Param("since") Instant since,
                                             Pageable pageable);
}
