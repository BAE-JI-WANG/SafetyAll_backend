# SafetyAll Backend

스마트 안전모(SafetyAll) 시스템의 백엔드 서버입니다. Java 21 / Spring Boot 4 / MySQL 기반입니다.

| 구성요소 | 저장소 |
|---|---|
| 안드로이드 앱 | [SafetyAll_App](https://github.com/BAE-JI-WANG/SafetyAll_App) |
| 웹 대시보드 | [SafetyAll](https://github.com/BAE-JI-WANG/SafetyAll) |
| 펌웨어 | [SatetyAll_HW-FW](https://github.com/BAE-JI-WANG/SatetyAll_HW-FW) |

```
안전모(ESP32) ─Bluetooth→ 안드로이드 앱 ──HTTP→ [이 서버] ←HTTP── 웹 대시보드(React)
                                                  │
                                                MySQL
```

## 이 서버가 하는 일

원래 서버(PHP, 비공개)가 제공하던 `Api/v1/Api.php?apicall=...` 주소·파라미터·응답 모양을 그대로 지원합니다.
**앱과 대시보드의 코드를 바꾸지 않고 주소만 이 서버로 바꾸면** 동작합니다.

- 앱이 보내는 것: 작업 시작, 상태 변경(착용/미착용), 경고(낙상·움직임 미감지·헬멧 연결 끊김·앱 강제 종료), 연결 해제
- 대시보드가 읽는 것: 현재 작업자, 주의(미착용), 처리되지 않은 경고, 경고 이력, 경고 처리 결과 입력

호출 목록과 응답 예시는 [docs/API.md](docs/API.md)에 있습니다.

> ⚠️ **원래 서버의 소스를 보지 못하고, 앱과 대시보드 코드가 보내고 읽는 형태에서 계약을 역추적해 만들었습니다.**
> 요청/응답 형식은 실제 앱·대시보드 클래스로 검증했지만, "경고" 처리 방식 같은 **서버 내부 의미는 추정**입니다.
> 추정한 부분과 원본과 다를 수 있는 부분은 [docs/API.md의 '가정과 확인이 필요한 부분'](docs/API.md#가정과-확인이-필요한-부분)에 정리했습니다.

## 실행하기

### Docker Compose (권장)

```bash
cp .env.example .env        # DB_PASSWORD, DB_ROOT_PASSWORD를 채운다
docker compose up -d --build
curl localhost:8080/actuator/health   # {"status":"UP",...}
```

MySQL과 서버가 함께 뜨고, 서버는 `127.0.0.1:8080`에만 열립니다. 아래 [보안](#보안)을 먼저 읽으세요.

### 직접 실행

MySQL 8.x가 필요합니다(문자셋 `utf8mb4`).

```bash
export DB_URL='jdbc:mysql://localhost:3306/safetyall?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'
export DB_USERNAME=safetyall
export DB_PASSWORD='...'            # 기본값 없음. 없으면 DB 접속에 실패해 서버가 뜨지 않는다
./gradlew bootRun                   # 테이블은 Flyway가 자동으로 만든다
```

### 설정 (환경 변수)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/safetyall?...` | `connectionTimeZone=UTC` 등 시간대 옵션을 유지하세요 |
| `DB_USERNAME` | `safetyall` | |
| `DB_PASSWORD` | 없음(필수) | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 대시보드 주소. 쉼표로 여러 개 |
| `SAFETYALL_TIMEZONE` | `Asia/Seoul` | 응답 `dateTime` 표시 시간대(DB에는 UTC로 저장) |
| `SAFETYALL_WARNING_DEDUPE_WINDOW` | `PT10S` | 같은 기기·같은 사유의 미처리 경고가 이 시간 안에 다시 오면 새로 만들지 않음 |
| `SAFETYALL_HISTORY_LIMIT` | `1000` | 경고 이력 조회 최대 건수(최신순) |
| `PORT` | `8080` | |

## 앱·대시보드 연결

- **안드로이드 앱** 빌드 설정 `API_BASE_URL` = `https://<서버>/Api/v1/Api.php?apicall=`
- **대시보드** 환경 변수 `REACT_APP_SAFE_API_KEY` = `https://<서버>/Api/v1/`

  대시보드는 `axios`로 `Api.php`를 붙여 호출하는 곳과, `fetch`로 이 값 뒤에 바로 `?apicall=`을 붙이는 곳이 섞여 있어서 **끝 슬래시를 포함**하는 형태가 둘 다 맞습니다. `/Api/v1`(슬래시 없음)도 받습니다.

앱 릴리스 빌드는 HTTPS만 허용하므로 서버 앞에 TLS를 끝내는 리버스 프록시(nginx, Caddy 등)가 필요합니다.

## 보안

**이 서버에는 인증이 없습니다.** 원래 서버와 같은 구조이고, 앱과 대시보드가 인증 정보를 보내지 않기 때문입니다.
따라서 주소를 아는 누구나 작업자 **이름·전화번호·위치를 조회**하고 경고를 **처리 완료로 바꿀 수 있습니다.**

- 가장 시급한 후속 작업은 인증(API 키 또는 로그인)입니다. 앱과 대시보드 수정이 함께 필요합니다.
- 그 전까지는 사내망/VPN, 리버스 프록시의 IP 제한이나 Basic 인증으로 접근을 제한하세요.
- 개인정보(이름·전화번호·위치)를 다루므로 보관 기간 정책이 필요합니다(아직 자동 삭제 없음).
- DB 비밀번호는 환경 변수로만 받으며 저장소에 올리지 않습니다(`.env`는 git에서 제외).

## 개발

```bash
./gradlew build        # 컴파일 + 전체 테스트
./gradlew test         # 테스트만
```

- 테스트 46개: H2(MySQL 모드)로 앱·대시보드 호출 흐름 전체를 검증하고, Docker가 있으면 **실제 MySQL 8.4(Testcontainers)** 로
  마이그레이션, utf8mb4 저장, UTC 저장/표시 시간대를 추가로 검증합니다(Docker가 없으면 이 부분만 건너뜁니다).
- JDK 21 필요. 코드 구조는 `api`(HTTP·검증) → `worker`/`warning`(도메인·서비스·저장소) 입니다.

## 알려진 한계 / 후속 작업

- 인증 없음(위 참고)
- 앱이 주기적 보고(heartbeat)를 보내지 않아, 연결 해제 없이 사라진 작업자는 계속 "작업 중"으로 남습니다. 앱 쪽 보고 추가 후 서버에서 만료 처리가 필요합니다.
- 응답 형식이 원본 PHP와의 호환을 위한 것이라 REST답지 않습니다. 앱·대시보드를 함께 개편할 때 정리하세요.
- 데이터 보관 기간/자동 삭제 없음
