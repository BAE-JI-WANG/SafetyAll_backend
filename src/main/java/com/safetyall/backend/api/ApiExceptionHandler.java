package com.safetyall.backend.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.safetyall.backend.common.InvalidRequestException;
import com.safetyall.backend.common.NotFoundException;

/**
 * 모든 오류를 {@code {"error": true, "message": "..."}}로 돌려준다.
 * 앱은 응답 본문을 읽지 않고 HTTP 상태로만 재시도 여부를 정하므로(5xx만 재시도) 상태 코드를 정확히 구분한다.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<Object> invalid(InvalidRequestException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> notFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 같은 기기가 동시에 시작 보고를 보내 유니크 제약에 걸린 경우. 앱이 재시도하지 않는 4xx로 알린다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> conflict(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return respond(HttpStatus.CONFLICT, "Conflicting request, please retry");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e) {
        log.error("Unhandled error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    /** 스프링이 직접 던지는 오류(잘못된 파라미터 형식 등)도 같은 본문으로 맞춘다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        String message = body instanceof ProblemDetail problem && problem.getDetail() != null
                ? problem.getDetail()
                : HttpStatus.valueOf(status.value()).getReasonPhrase();
        return super.handleExceptionInternal(ex, ApiBody.error(message), headers, status, request);
    }

    private static ResponseEntity<Object> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiBody.error(message));
    }
}
