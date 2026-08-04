package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.ai.domain.exception.AiReportTrialExceededException;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 분석 리포트. MVP에서는 분석 결과를 통 리포트로 저장한다 (명세 미확정 — 세분화 시 분해).
 */
@Getter
@Entity
@Table(name = "ai_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport extends BaseTimeEntity {

	// MVP 무료 체험 정책: 사용자당 리포트 생성을 총 3회로 제한한다
	public static final int MAX_TRIAL_COUNT = 3;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	// presigned URL은 만료되고 공개 URL은 도메인 변경에 흔들리므로, 불변인 S3 객체 키를 안정적 식별자로 저장한다
	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	private AiReport(AiReportCreateCommand command) {
		this.userId = command.userId();
		this.videoObjectKey = command.videoObjectKey();
		this.content = command.content();
	}

	public static AiReport create(AiReportCreateCommand command) {
		return new AiReport(command);
	}

	/**
	 * 생성된 리포트 수가 무료 체험 한도 미만인지 검증한다. 소진했으면 예외를 던진다.
	 */
	public static void validateTrialAvailable(long generatedCount) {
		if (generatedCount >= MAX_TRIAL_COUNT) {
			throw new AiReportTrialExceededException();
		}
	}
}
