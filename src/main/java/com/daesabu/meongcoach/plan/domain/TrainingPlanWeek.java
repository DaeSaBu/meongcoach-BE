package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "training_plan_weeks", uniqueConstraints = @UniqueConstraint(
		name = "uk_training_plan_weeks_plan_id_week_no", columnNames = {"plan_id", "week_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingPlanWeek extends BaseEntity {

	static final int THEME_MAX_LENGTH = 20;
	static final int MILESTONE_MAX_LENGTH = 40;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private TrainingPlan plan;

	@Column(nullable = false)
	private int weekNo;

	@Column(nullable = false, length = THEME_MAX_LENGTH)
	private String theme;

	@Column(nullable = false, length = MILESTONE_MAX_LENGTH)
	private String milestone;

	private TrainingPlanWeek(TrainingPlan plan, int weekNo, String theme, String milestone) {
		this.plan = plan;
		this.weekNo = weekNo;
		this.theme = TrainingPlan.truncate(theme, THEME_MAX_LENGTH);
		this.milestone = TrainingPlan.truncate(milestone, MILESTONE_MAX_LENGTH);
	}

	static TrainingPlanWeek create(TrainingPlan plan, int weekNo, String theme, String milestone) {
		return new TrainingPlanWeek(plan, weekNo, theme, milestone);
	}
}
