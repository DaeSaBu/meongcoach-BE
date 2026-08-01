package com.daesabu.meongcoach.ai.domain;

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

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	// presigned URL은 만료되므로 만료 없는 공개 URL을 안정적 식별자로 저장한다
	@Column(nullable = false, length = 512)
	private String videoUrl;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	private AiReport(AiReportCreateCommand command) {
		this.userId = command.userId();
		this.videoUrl = command.videoUrl();
		this.content = command.content();
	}

	public static AiReport create(AiReportCreateCommand command) {
		return new AiReport(command);
	}
}
