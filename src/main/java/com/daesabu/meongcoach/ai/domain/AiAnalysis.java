package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 영상 분석 요청. 상태 전이가 잦은 테이블이다.
 */
@Getter
@Entity
@Table(name = "ai_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysis extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	// 강아지 미지정 분석 업로드를 허용하므로 nullable
	private Long dogId;

	// presigned S3 업로드 URL
	@Column(nullable = false, length = 512)
	private String videoUrl;

	// 길이/용량 제한 검증에 사용한다 (U-0404)
	@Column(nullable = false)
	private Integer videoLengthSec;

	@Column(nullable = false)
	private Long fileSizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AiAnalysisStatus status;

	public static AiAnalysis upload(Long userId, Long dogId, String videoUrl, Integer videoLengthSec,
	                                Long fileSizeBytes) {
		AiAnalysis analysis = new AiAnalysis();
		analysis.userId = userId;
		analysis.dogId = dogId;
		analysis.videoUrl = videoUrl;
		analysis.videoLengthSec = videoLengthSec;
		analysis.fileSizeBytes = fileSizeBytes;
		analysis.status = AiAnalysisStatus.UPLOADED;
		return analysis;
	}

	public void startProcessing() {
		this.status = AiAnalysisStatus.PROCESSING;
	}

	public void complete() {
		this.status = AiAnalysisStatus.COMPLETED;
	}

	public void fail() {
		this.status = AiAnalysisStatus.FAILED;
	}
}
