package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_analyses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysis extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiAnalysisStatus status;

	private Instant uploadExpiresAt;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "report_id", unique = true)
	private AiReport report;

	public static AiAnalysis uploading(AiAnalysisUploadCommand command) {
		AiAnalysis analysis = new AiAnalysis();
		analysis.userId = command.userId();
		analysis.videoObjectKey = command.videoObjectKey();
		analysis.uploadExpiresAt = command.uploadExpiresAt();
		analysis.status = AiAnalysisStatus.UPLOADING;
		return analysis;
	}

	public boolean isUploading() {
		return status == AiAnalysisStatus.UPLOADING;
	}

	public void startAnalysis() {
		this.status = AiAnalysisStatus.ANALYZING;
	}

	public AiAnalysisStatus statusAt(Instant now) {
		if (isUploading() && now.isAfter(uploadExpiresAt)) {
			return AiAnalysisStatus.FAILED_UPLOAD;
		}
		return status;
	}

	public void complete(String title, String content) {
		this.report = AiReport.of(title, content);
		this.status = AiAnalysisStatus.COMPLETED;
	}

	public void failByTrialExceeded() {
		this.status = AiAnalysisStatus.FAILED_TRIAL_EXCEEDED;
	}

	public void failByAnalysis() {
		this.status = AiAnalysisStatus.FAILED_ANALYSIS;
	}

	public void failUnexpectedly() {
		this.report = null;
		this.status = AiAnalysisStatus.FAILED_UNEXPECTED;
	}

	public String getTitle() {
		if (report == null) {
			return null;
		}
		return report.getTitle();
	}

	public String getContent() {
		if (report == null) {
			return null;
		}
		return report.getContent();
	}
}
