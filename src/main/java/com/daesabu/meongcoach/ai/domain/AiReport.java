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

/**
 * AI 분석 리포트. MVP에서는 분석 결과를 통 리포트로 저장한다 (명세 미확정 — 세분화 시 분해).
 * 업로드 영상 1건당 row 1개가 PENDING으로 생성되고, 분석 결말에 따라 COMPLETED 또는 FAILED_* 로 전이한다.
 * 제목·본문은 COMPLETED로 전이할 때만 채워진다.
 */
@Getter
@Entity
@Table(name = "ai_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport extends BaseTimeEntity {

	// 제목 길이 규칙의 단일 출처. 컬럼 DDL과 정규화가 같은 상수를 참조해 어긋날 수 없다
	public static final int TITLE_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	// presigned URL은 만료되고 공개 URL은 도메인 변경에 흔들리므로, 불변인 S3 객체 키를 안정적 식별자로 저장한다
	@Column(nullable = false, length = 512)
	private String videoObjectKey;

	// 제목 생성은 부가 기능이라 실패해도 리포트는 저장해야 하므로 null을 허용한다
	@Column(length = TITLE_MAX_LENGTH)
	private String title;

	// 본문은 COMPLETED일 때만 채워지므로 PENDING·FAILED row에서는 null이다
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

	/**
	 * 영상 소유자를 확인한 직후 분석 진행 중 상태로 만든다. 제목·본문은 complete()에서 채운다.
	 */
	public static AiReport pending(Long userId, String videoObjectKey) {
		return new AiReport(userId, videoObjectKey);
	}

	/**
	 * 분석 성공을 기록한다. 제목은 부가 정보라 null·규칙 위반을 거부하지 않고 저장 가능한 형태로 정규화한다.
	 */
	public void complete(String title, String content) {
		this.title = normalizeTitle(title);
		this.content = content;
		this.status = AiReportStatus.COMPLETED;
	}

	/**
	 * 실패 결말을 기록한다. 진행 중·성공 상태를 실패로 넘기려는 호출은 프로그래밍 오류라 거부한다.
	 */
	public void fail(AiReportStatus status) {
		if (!status.isFailure()) {
			throw new IllegalArgumentException("실패 상태가 아닌 값으로 리포트를 실패 처리할 수 없다: " + status);
		}
		this.status = status;
	}

	// 제목은 부가 정보라 규칙 위반을 거부하지 않고 저장 가능한 형태로 정규화한다.
	// 예외를 던지면 제목 때문에 리포트 전체가 유실된다
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
