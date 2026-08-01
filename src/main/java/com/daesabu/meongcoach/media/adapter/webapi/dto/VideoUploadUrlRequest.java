package com.daesabu.meongcoach.media.adapter.webapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VideoUploadUrlRequest(
		@NotBlank String target,
		@NotBlank String contentType,
		// 크기 위반은 전용 에러 코드로 내려야 해서 값 범위 검증은 도메인(VideoFileSize)이 맡는다
		@NotNull Long fileSizeBytes) {
}
