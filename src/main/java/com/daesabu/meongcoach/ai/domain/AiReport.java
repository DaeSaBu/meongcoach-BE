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

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiReportStatus status;

	private AiReport(AiReportCreateCommand command) {
		this.userId = command.userId();
		this.videoObjectKey = command.videoObjectKey();
		this.title = normalizeTitle(command.title());
		this.content = command.content();
		// create()는 분석 성공 시에만 호출되므로 상태는 호출자 입력이 아니라 도메인이 결정한다
		this.status = AiReportStatus.COMPLETED;
	}

	public static AiReport create(AiReportCreateCommand command) {
		return new AiReport(command);
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
