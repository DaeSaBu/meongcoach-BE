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
@Table(name = "training_plan_items", uniqueConstraints = @UniqueConstraint(
		name = "uk_training_plan_items_plan_id_curriculum_id", columnNames = {"plan_id", "curriculum_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingPlanItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private TrainingPlan plan;

	@Column(nullable = false)
	private int weekNo;

	@Column(nullable = false)
	private Long curriculumId;

	@Column(nullable = false)
	private int sortOrder;

	private TrainingPlanItem(TrainingPlan plan, int weekNo, Long curriculumId, int sortOrder) {
		this.plan = plan;
		this.weekNo = weekNo;
		this.curriculumId = curriculumId;
		this.sortOrder = sortOrder;
	}

	static TrainingPlanItem create(TrainingPlan plan, int weekNo, Long curriculumId, int sortOrder) {
		return new TrainingPlanItem(plan, weekNo, curriculumId, sortOrder);
	}
}
