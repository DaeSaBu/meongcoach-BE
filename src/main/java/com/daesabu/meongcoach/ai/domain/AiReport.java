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

	private AiReport(Long userId, String videoObjectKey) {
		this.userId = userId;
		this.videoObjectKey = videoObjectKey;
		this.status = AiReportStatus.PENDING;
	}

	public static AiReport pending(Long userId, String videoObjectKey) {
		return new AiReport(userId, videoObjectKey);
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
