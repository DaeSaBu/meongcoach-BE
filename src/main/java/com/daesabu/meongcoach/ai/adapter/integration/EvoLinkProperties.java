package com.daesabu.meongcoach.ai.adapter.integration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 영상 분석에 쓰는 EvoLink.AI 채팅 API(OpenAI 호환) 연동 설정.
 * 설정 오류는 모두 기동 시점에 드러나야 하므로 모든 필드에 제약을 건다.
 *
 * @param baseUrl         EvoLink API 베이스 URL. 멀티모달(영상) 호출은 api.evolink.ai가 주력 엔드포인트다
 * @param apiKey          EvoLink API 키. Bearer 토큰으로 전송한다
 * @param model           호출할 모델 이름 (예: doubao-seed-2.0-pro)
 * @param responseTimeout 모델 응답을 기다리는 시간. 영상 분석은 수십 초가 걸려 전역 read-timeout(3초)으로는 짧다
 * @param maxTokens       응답 최대 토큰 수. 상한에 닿으면 리포트가 문장 중간에서 잘리므로 리포트 분량보다 넉넉히 둔다
 * @param temperature     응답 샘플링 온도. 분석 리포트는 창의성보다 일관성이 중요해 낮게 둔다
 * @param thinking        심층 사고 모드 (enabled·disabled·auto). 비용·지연 예측을 위해 기본은 disabled로 둔다
 * @param videoFps        영상 프레임 추출 빈도(0.2~5). 높을수록 화면 변화에 민감하지만 토큰 소비가 늘어난다
 */
@Validated
@ConfigurationProperties("meongcoach.ai.evolink")
public record EvoLinkProperties(
		@NotBlank String baseUrl,
		@NotBlank String apiKey,
		@NotBlank String model,
		@NotNull Duration responseTimeout,
		@NotNull @Positive Integer maxTokens,
		@NotNull @PositiveOrZero Double temperature,
		@NotBlank String thinking,
		@NotNull @DecimalMin("0.2") @DecimalMax("5.0") Double videoFps
) {
}
