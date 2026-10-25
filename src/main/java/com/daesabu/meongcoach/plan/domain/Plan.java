package com.daesabu.meongcoach.plan.domain;

import static java.util.Objects.requireNonNull;
import static org.springframework.util.Assert.state;

import com.daesabu.meongcoach.plan.domain.exception.DuplicatePlanCurriculumException;
import com.daesabu.meongcoach.plan.domain.exception.EmptyPlanCurriculumsException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDailyMinutesException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidDaysPerWeekException;
import com.daesabu.meongcoach.plan.domain.exception.InvalidPlanCurriculumException;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "plans")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseTimeEntity {

	private static final Set<Integer> ALLOWED_DAILY_MINUTES = Set.of(5, 10, 20);

	private static final Set<Integer> ALLOWED_DAYS_PER_WEEK = Set.of(3, 5, 7);

	private static final String CUSTOM_TITLE_SUFFIX = "의 2주 훈련 플랜";

	private static final String CUSTOM_GOAL = "선택한 훈련을 차근차근 익혀요";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long dogId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String goal;

	@Column(nullable = false)
	private int dailyMinutes;

	@Column(nullable = false)
	private int daysPerWeek;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanStatus status;

	private LocalDateTime completedAt;

	@ElementCollection
	@CollectionTable(name = "plan_curriculums", joinColumns = @JoinColumn(name = "plan_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_plan_curriculums_plan_curriculum",
					columnNames = {"plan_id", "curriculum_id"}))
	@Column(name = "curriculum_id", nullable = false)
	@OrderColumn(name = "sort_order", nullable = false)
	private List<Long> curriculumIds = new ArrayList<>();

	public static Plan create(PlanCreateCommand command, List<Long> curriculumIds) {
		validateDailyMinutes(command.dailyMinutes());
		validateDaysPerWeek(command.daysPerWeek());
		validateCurriculumIds(curriculumIds);

		Plan plan = new Plan();
		plan.userId = requireNonNull(command.userId());
		plan.dogId = requireNonNull(command.dogId());
		plan.title = requireNonNull(command.title());
		plan.goal = requireNonNull(command.goal());
		plan.dailyMinutes = command.dailyMinutes();
		plan.daysPerWeek = command.daysPerWeek();
		plan.status = PlanStatus.ACTIVE;
		plan.curriculumIds = new ArrayList<>(curriculumIds);
		return plan;
	}

	public static Plan createCustom(CustomPlanCreateCommand command, List<Long> curriculumIds) {
		String title = requireNonNull(command.dogName()) + CUSTOM_TITLE_SUFFIX;
		PlanCreateCommand createCommand = new PlanCreateCommand(command.userId(), command.dogId(), title,
				CUSTOM_GOAL, command.dailyMinutes(), command.daysPerWeek());
		return create(createCommand, curriculumIds);
	}

	public List<Long> getCurriculumIds() {
		return Collections.unmodifiableList(curriculumIds);
	}

	public void complete(LocalDateTime completedAt) {
		state(status == PlanStatus.ACTIVE, "활성 플랜만 완료할 수 있습니다.");
		this.completedAt = requireNonNull(completedAt);
		status = PlanStatus.COMPLETED;
	}

	public void replace() {
		state(status == PlanStatus.ACTIVE, "활성 플랜만 교체할 수 있습니다.");
		status = PlanStatus.REPLACED;
	}

	private static void validateDailyMinutes(int dailyMinutes) {
		if (!ALLOWED_DAILY_MINUTES.contains(dailyMinutes)) {
			throw new InvalidDailyMinutesException();
		}
	}

	private static void validateCurriculumIds(List<Long> curriculumIds) {
		requireNonNull(curriculumIds);
		if (curriculumIds.isEmpty()) {
			throw new EmptyPlanCurriculumsException();
		}
		if (curriculumIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidPlanCurriculumException();
		}
		Set<Long> uniqueCurriculumIds = new HashSet<>(curriculumIds);
		if (uniqueCurriculumIds.size() != curriculumIds.size()) {
			throw new DuplicatePlanCurriculumException();
		}
	}

	private static void validateDaysPerWeek(int daysPerWeek) {
		if (!ALLOWED_DAYS_PER_WEEK.contains(daysPerWeek)) {
			throw new InvalidDaysPerWeekException();
		}
	}
}
