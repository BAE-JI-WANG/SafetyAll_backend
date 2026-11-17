package com.safetyall.backend.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 서버 동작 설정. 모든 값에 기본값이 있고 환경 변수/프로퍼티로 바꿀 수 있다.
 *
 * @param timezone            응답의 dateTime 문자열에 쓰는 표시용 시간대. DB에는 항상 UTC로 저장한다.
 * @param warningDedupeWindow 같은 기기에서 같은 사유의 미처리 경고가 이 시간 안에 다시 오면 새로 만들지 않는다.
 *                            앱이 응답을 못 받고 재전송해도 경고가 중복되지 않게 하기 위한 값이다.
 * @param historyLimit        경고 이력 조회가 돌려주는 최대 건수(최신순).
 */
@ConfigurationProperties("safetyall")
public record AppProperties(
        @DefaultValue("Asia/Seoul") ZoneId timezone,
        @DefaultValue("PT10S") Duration warningDedupeWindow,
        @DefaultValue("1000") int historyLimit,
        @DefaultValue Cors cors) {

    public AppProperties {
        if (warningDedupeWindow.isNegative()) {
            throw new IllegalArgumentException("safetyall.warning-dedupe-window must not be negative");
        }
        if (historyLimit < 1) {
            throw new IllegalArgumentException("safetyall.history-limit must be at least 1");
        }
    }

    /** 웹 대시보드가 다른 출처에서 호출하므로 허용할 출처를 지정한다. */
    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {
    }
}
