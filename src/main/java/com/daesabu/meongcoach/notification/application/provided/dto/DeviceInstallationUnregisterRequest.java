package com.daesabu.meongcoach.notification.application.provided.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그아웃 때 본인 기기 FID을 지우는 요청. FID은 그 기기로 알림을 보낼 수 있는 값이라 URL이 아니라 본문으로 받아 접근 로그에 남기지 않는다.
 */
public record DeviceInstallationUnregisterRequest(@NotBlank @Size(max = 512) String fid) {
}
