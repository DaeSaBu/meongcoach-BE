package com.daesabu.meongcoach.report.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.report.domain.shared.ReportTargetType;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "reports",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_reports_reporter_target", columnNames = {"reporter_id", "target_type", "target_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long reporterId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReportTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ReportReason reason;

	@Column(length = 500)
	private String detail;

	public static Report register(ReportRegisterCommand command) {
		Report report = new Report();

		report.reporterId = requireNonNull(command.reporterId());
		report.targetType = requireNonNull(command.targetType());
		report.targetId = requireNonNull(command.targetId());
		report.reason = requireNonNull(command.reason());
		report.detail = command.detail();

		return report;
	}
}
