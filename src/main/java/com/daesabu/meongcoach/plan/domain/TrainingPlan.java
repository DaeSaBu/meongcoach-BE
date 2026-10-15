package com.daesabu.meongcoach.plan.domain;

import com.daesabu.meongcoach.plan.domain.PlanDraft.WeekDraft;
import com.daesabu.meongcoach.plan.domain.exception.PlanNotActiveException;
import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "training_plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingPlan extends BaseEntity {

	private static final int TITLE_MAX_LENGTH = 20;
	private static final int GOAL_MAX_LENGTH = 60;
	private static final int REASON_MAX_LENGTH = 150;
	private static final int DAYS_PER_WEEK = 7;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long dogId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanMode mode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanGeneratorType generator;

	@Column(nullable = false, length = TITLE_MAX_LENGTH)
	private String title;

	@Column(nullable = false, length = GOAL_MAX_LENGTH)
	private String goal;

	@Column(nullable = false, length = REASON_MAX_LENGTH)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private PlanAdjustedReason adjustedReason;

	@Column(nullable = false)
	private int durationWeeks;

	@Column(nullable = false)
	private int dailyMinutes;

	@Column(nullable = false)
	private int daysPerWeek;

	@Column(nullable = false)
	private LocalDate startedOn;

	@Column(nullable = false)
	private LocalDate targetEndOn;

	private LocalDateTime completedAt;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private PlanOutcome outcome;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String interviewJson;

	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("weekNo ASC")
	private List<TrainingPlanWeek> weeks = new ArrayList<>();

	@OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("weekNo ASC, sortOrder ASC")
	private List<TrainingPlanItem> items = new ArrayList<>();

	private TrainingPlan(TrainingPlanCreateCommand command) {
		PlanDraft draft = command.draft();
		this.userId = command.userId();
		this.dogId = command.dogId();
		this.status = PlanStatus.ACTIVE;
		this.mode = command.mode();
		this.generator = command.generator();
		this.title = truncate(draft.title(), TITLE_MAX_LENGTH);
		this.goal = truncate(draft.goal(), GOAL_MAX_LENGTH);
		this.reason = truncate(draft.reason(), REASON_MAX_LENGTH);
		this.adjustedReason = command.adjustedReason();
		this.durationWeeks = command.durationWeeks();
		this.dailyMinutes = command.dailyMinutes();
		this.daysPerWeek = command.daysPerWeek();
		this.startedOn = command.startedOn();
		this.targetEndOn = command.startedOn().plusDays((long) command.durationWeeks() * DAYS_PER_WEEK - 1);
		this.interviewJson = command.interviewJson();
		draft.weeks().forEach(this::addWeek);
	}

	public static TrainingPlan create(TrainingPlanCreateCommand command) {
		return new TrainingPlan(command);
	}

	public void abandon() {
		if (status != PlanStatus.ACTIVE) {
			throw new PlanNotActiveException();
		}
		this.status = PlanStatus.ABANDONED;
	}

	public void complete(PlanOutcome outcome, LocalDateTime now) {
		if (status != PlanStatus.ACTIVE) {
			throw new PlanNotActiveException();
		}
		this.status = PlanStatus.COMPLETED;
		this.outcome = outcome;
		this.completedAt = now;
	}

	public List<Long> curriculumIds() {
		return sortedItems().stream()
				.map(TrainingPlanItem::getCurriculumId)
				.toList();
	}

	public List<TrainingPlanItem> itemsOf(int weekNo) {
		return sortedItems().stream()
				.filter(item -> item.getWeekNo() == weekNo)
				.toList();
	}

	static String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private void addWeek(WeekDraft draft) {
		weeks.add(TrainingPlanWeek.create(this, draft.weekNo(), draft.theme(), draft.milestone()));
		int sortOrder = 1;
		for (Long curriculumId : draft.curriculumIds()) {
			items.add(TrainingPlanItem.create(this, draft.weekNo(), curriculumId, sortOrder));
			sortOrder++;
		}
	}

	private List<TrainingPlanItem> sortedItems() {
		return items.stream()
				.sorted(Comparator.comparingInt(TrainingPlanItem::getWeekNo)
						.thenComparingInt(TrainingPlanItem::getSortOrder))
				.toList();
	}
}
