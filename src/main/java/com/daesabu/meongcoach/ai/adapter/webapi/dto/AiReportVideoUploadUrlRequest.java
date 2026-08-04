package com.daesabu.meongcoach.ai.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiReportVideoUploadUrlRequest(
		@NotBlank String contentType,
		// 크기 위반은 전용 에러 코드로 내려야 해서 값 범위 검증은 media 도메인(VideoFileSize)이 맡는다
		@NotNull Long fileSizeBytes) {
}
