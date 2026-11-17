package com.safetyall.backend.worker;

/**
 * 앱이 보내는 상태 문자열. 안드로이드 앱(ReportPlanner)과 대시보드가 이 값을 그대로 사용한다.
 * 상태는 자유 문자열로 저장하고(이전 버전 앱은 "낙상" 같은 값도 보냈다), 아래 값만 의미를 가진다.
 */
public final class WorkerStatus {

    /** 헬멧을 벗은 상태. 대시보드의 "주의"로 집계한다. */
    public static final String CAUTION = "미착용";

    /** 낙상/움직임 미감지/연결 끊김/앱 종료. 경고 이력의 기본 상태이기도 하다. */
    public static final String WARNING = "경고";

    /** 대시보드에서 담당자가 경고를 처리하면 경고 이력의 상태가 이 값이 된다. */
    public static final String RESOLVED = "처리완료";

    private WorkerStatus() {
    }
}
