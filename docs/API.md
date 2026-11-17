# API 명세

원래 PHP 서버와 호환되는 단일 엔드포인트입니다. 모든 호출은 `apicall` 쿼리 파라미터로 구분합니다.

## 주소

세 가지 경로가 모두 같은 동작을 합니다(앱·대시보드가 서로 다른 형태로 부르기 때문).

```
/Api/v1/Api.php?apicall=...      안드로이드 앱, 대시보드(axios)
/Api/v1/?apicall=...             대시보드(fetch: 환경 변수 주소 + ?apicall=)
/Api/v1?apicall=...              끝 슬래시 없는 설정
```

## 공통

- 성공: HTTP 200, 본문 `{"error": false, "message": "...", ...데이터}`
- 실패: 상태 코드에 맞는 본문 `{"error": true, "message": "..."}`

| 상태 | 의미 |
|---|---|
| 400 | 파라미터 오류, 없거나 모르는 `apicall`, 맞지 않는 HTTP 메서드(예: GET으로 `insertWorker`) |
| 404 | 없는 경고 번호(`updateWarningResult`), 지원하지 않는 경로 |
| 409 | 동시 요청으로 인한 저장 충돌(다시 시도하면 됨) |
| 500 | 서버 오류 |

앱은 응답 본문을 읽지 않고 **상태 코드로만** 재시도 여부를 정합니다(5xx와 네트워크 오류만 최대 3회 재시도, 4xx는 재시도 안 함).
그래서 잘못된 요청은 반드시 4xx로, 서버 문제는 5xx로 구분해 돌려줍니다.

`message` 문자열은 사람이 읽는 용도이며 클라이언트는 읽지 않습니다. 표시 시각 `dateTime`은 `yyyy-MM-dd HH:mm:ss`(기본 Asia/Seoul)입니다.

## 안드로이드 앱

요청 본문은 `application/x-www-form-urlencoded; charset=UTF-8` 입니다.

### 공통 파라미터 (`insertWorker`, `updateWorkerStatus`, `insertWorkerWarning`)

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `deviceId` | ✅ | 기기 식별자(ANDROID_ID). 최대 64자 |
| `status` | `insertWorker`, `updateWorkerStatus`만 ✅ | 작업시작 / 착용 / 미착용 / 경고. 최대 30자. `insertWorkerWarning`은 없으면 `경고` |
| `reason` | `insertWorkerWarning`만 ✅ | 낙상 / 움직임 미감지 / 헬멧 연결 끊김 / 어플강제종료. 최대 100자 |
| `workerName`, `workerPhoneNum`, `workerGroup` | | 없거나 비어 있으면 서버가 아는 작업자 정보로 채우고, 길면 50/30/50자로 잘라 저장(거부하지 않음) |
| `latitude`, `longitude` | | 없으면 위치 모름. 숫자가 아니거나 범위(±90, ±180) 밖이면 400 |

위치 `0.0, 0.0`은 "위치를 알 수 없음"으로 봅니다(앱이 GPS를 못 얻으면 그 값을 보냅니다). 이때는 그 작업자가 마지막으로 알려진 위치를 유지하며,
경고도 마지막 위치를 사용합니다.

| 호출 | 메서드 | 동작 |
|---|---|---|
| `insertWorker` | POST | 작업 시작. 같은 `deviceId`가 있으면 새 작업으로 갱신 |
| `updateWorkerStatus` | POST | 상태 갱신. 서버가 모르는 기기이거나 이미 종료된 기기여도 새로 등록/되살림(보고를 버리지 않기 위해) |
| `insertWorkerWarning` | POST | 경고 기록. 응답에 `idx`(경고 번호) 포함 |
| `deleteWorker` | GET `&deviceId=` | 연결 해제. 이미 종료되었거나 모르는 기기여도 200(재전송 대비) |

`updateWorkerStatus`가 `reason`을 함께 보내도 무시합니다.

**경고 중복 방지**: 같은 `deviceId`·같은 `reason`의 *처리되지 않은* 경고가 `SAFETYALL_WARNING_DEDUPE_WINDOW`(기본 10초) 안에 이미 있으면
새로 만들지 않고 기존 경고의 `idx`를 돌려줍니다. 앱이 응답을 못 받고 같은 요청을 다시 보내도 경고가 두 번 뜨지 않게 하기 위함입니다.
같은 사유라도 처리된 뒤이거나 시간이 지났으면 새 경고입니다. 위험이 계속되어 앱이 일부러 반복 전송하는 경우를 막지 않습니다.

## 대시보드

모두 GET이며 2~3초마다 호출합니다.

| 호출 | 응답 키 | 내용 |
|---|---|---|
| `selectWorker` | `workers[]` | 현재 작업 중인 작업자 |
| `selectWorkerCaution` | `workersCaution[]`, `workers[]` | 헬멧을 벗은(`미착용`) 작업자. 두 키 모두 같은 내용 |
| `selectDReadWarning` | `DReadWarning[]` | **처리되지 않은** 경고(오래된 순). 개수가 "경고" 카드·알림 팝업을 움직임 |
| `selectWorkerWarning` | `workersWarning[]`, `workers[]` | `workersWarning`: 전체 경고 이력(최신순, 최대 `SAFETYALL_HISTORY_LIMIT`건). `workers`: 처리되지 않은 경고의 위치(지도용) |
| `updateWarningResult` | — | POST(multipart 또는 form) `idx`(양의 정수), `result`(필수, 최대 500자). 경고를 처리 완료로 바꿈. 없는 번호는 404 |

작업자 항목: `workerName, workerGroup, workerPhoneNum, status, latitude, longitude, dateTime`
경고 항목: `idx, workerName, workerGroup, workerPhoneNum, status, reason, result, latitude, longitude, dateTime`

`deviceId`는 응답에 포함하지 않습니다.

### 응답 키가 두 개씩 있는 이유

대시보드 코드가 같은 호출에서 **화면마다 다른 키**를 읽습니다. 예를 들어 `selectWorkerCaution`은 개수를 `workersCaution`에서, 지도 마커를 `workers`에서 읽고,
`selectWorkerWarning`은 이력 표를 `workersWarning`에서, 지도를 `workers`에서 읽습니다. 원래 서버가 어떻게 채웠는지 알 수 없으므로 **양쪽 키를 모두 채워**
카드 숫자와 지도 마커 개수가 어긋나지 않게 했습니다.

## 가정과 확인이 필요한 부분

원래 서버의 소스를 볼 수 없어 아래는 앱·대시보드 코드에서 **추정**한 것입니다. 원본을 아는 분이 확인해 주세요.

| 항목 | 이 서버의 구현(추정) |
|---|---|
| "경고"의 의미 | 경고는 `status=경고`로 저장되고, 대시보드에서 `updateWarningResult`로 결과를 입력하기 전까지 "처리되지 않은 경고". 처리하면 상태가 `처리완료`, `result`에 입력 내용 |
| "주의"의 의미 | 작업 중인 작업자 중 `status=미착용` |
| "현재 작업자" | 연결 해제(`deleteWorker`)하지 않은 작업자. 앱이 주기 보고를 보내지 않으므로 사라진 작업자는 계속 남음 |
| `deleteWorker` | 삭제가 아니라 종료 시각만 기록(경고 이력은 유지) |
| `dateTime` | 작업자는 마지막 갱신 시각, 경고는 발생 시각 |
| 응답 `message` | 임의의 영어 문구(클라이언트가 읽지 않음) |

**원본에 없는 이 서버의 추가 동작**: 경고 중복 방지, 위치 미확인(0,0) 처리, 비어 있는 신원 보정, 입력 검증(범위·길이), 이력 건수 제한, CORS 허용 출처 제한.

## 검증 범위

- 자동 테스트 46개: H2(MySQL 모드)로 앱·대시보드 호출 흐름, 검증, 중복 방지, URL 세 형태, CORS. Docker가 있으면 실제 MySQL 8.4(Testcontainers)로 마이그레이션·utf8mb4·UTC 저장을 추가 검증.
- 수동 검증: 실제 MySQL 8.4에서 jar와 컨테이너를 기동하고, **실제 앱 코어 클래스**(`StatusReporter`/`ApiClient`/`HttpRequester`)와 **실제 대시보드 데이터 모듈**
  (`countdata`, `positiondata`, `tabledata`, `historydata`, `reasoninput`)을 이 서버에 붙여 작업 시작 → 낙상·연결 끊김·앱 종료 경고 → 대시보드 조회 → 처리 입력 → 연결 해제까지 확인했습니다.
- **하지 못한 것**: 실제 안전모/휴대폰/브라우저 화면, 운영 환경의 TLS·부하·장시간 운영, `docker compose` 실행(이 환경에 compose가 없어 문법만 확인), GitHub Actions CI 실행.
