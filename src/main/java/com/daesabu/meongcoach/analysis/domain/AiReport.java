package com.daesabu.meongcoach.analysis.domain;

import static java.util.Objects.requireNonNull;

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

@Getter
@Entity
@Table(name = "ai_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport extends BaseTimeEntity {

	public static final int TITLE_MAX_LENGTH = 200;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = TITLE_MAX_LENGTH)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	static AiReport of(String title, String content) {
		AiReport report = new AiReport();
		report.title = normalizeTitle(title);
		report.content = requireNonNull(content);
		return report;
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
