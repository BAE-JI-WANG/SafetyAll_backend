package com.safetyall.backend.common;

/**
 * 위치. 안드로이드 앱은 위치를 알 수 없으면 0.0, 0.0을 보내므로 (0, 0)을 "알 수 없음"으로 취급한다.
 */
public record Location(double latitude, double longitude) {

    public boolean isUnknown() {
        return latitude == 0.0 && longitude == 0.0;
    }
}
