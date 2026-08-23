package com.daesabu.meongcoach.ai.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport extends BaseTimeEntity {

	public static final int TITLE_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	@Column(length = TITLE_MAX_LENGTH)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiReportStatus status;

	// 발급 시점 도입 전에 만들어진 row는 null이다
	private LocalDateTime uploadExpiresAt;

	private AiReport(AiReportUploadCommand command) {
		this.userId = command.userId();
		this.videoObjectKey = command.videoObjectKey();
		this.uploadExpiresAt = command.uploadExpiresAt();
		this.status = AiReportStatus.UPLOADING;
	}

	public static AiReport uploading(AiReportUploadCommand command) {
		return new AiReport(command);
	}

	public boolean isUploading() {
		return status == AiReportStatus.UPLOADING;
	}

	public void startAnalysis() {
		this.status = AiReportStatus.PENDING;
	}

	public AiReportStatus statusAt(LocalDateTime now) {
		if (isUploading() && now.isAfter(uploadExpiresAt)) {
			return AiReportStatus.FAILED_UPLOAD;
		}
		return status;
	}

	public void complete(String title, String content) {
		this.title = normalizeTitle(title);
		this.content = content;
		this.status = AiReportStatus.COMPLETED;
	}

	public void failByTrialExceeded() {
		this.status = AiReportStatus.FAILED_TRIAL_EXCEEDED;
	}

	public void failByAnalysis() {
		this.status = AiReportStatus.FAILED_ANALYSIS;
	}

	public void failUnexpectedly() {
		this.status = AiReportStatus.FAILED_UNEXPECTED;
	}

	private static String normalizeTitle(String title) {
		if (title == null) {
			return null;
		}
		String stripped = title.strip();
		if (stripped.isEmpty()) {
			return null;
		}
		if (stripped.length() > TITLE_MAX_LENGTH) {
			return stripped.substring(0, TITLE_MAX_LENGTH);
		}
		return stripped;
	}
}
