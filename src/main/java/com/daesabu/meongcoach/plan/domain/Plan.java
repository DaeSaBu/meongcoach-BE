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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

	private static final String TITLE_SUFFIX = "의 2주 훈련 플랜";

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

	@Column(length = 500)
	private String expectation;

	@Column(nullable = false)
	private int dailyMinutes;

	@Column(nullable = false)
	private int daysPerWeek;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PlanStatus status;

	private Instant completedAt;

	@ElementCollection
	@CollectionTable(name = "plan_curriculums", joinColumns = @JoinColumn(name = "plan_id"),
			uniqueConstraints = @UniqueConstraint(name = "uk_plan_curriculums_plan_curriculum",
					columnNames = {"plan_id", "curriculum_id"}))
	@Column(name = "curriculum_id", nullable = false)
	@OrderColumn(name = "sort_order", nullable = false)
	private List<Long> curriculumIds = new ArrayList<>();

	@Column(name = "completed_lesson_ids", nullable = false, columnDefinition = "bigint[]")
	private Set<Long> completedLessonIds = new HashSet<>();

	public static Plan create(PlanCreateCommand command, List<Long> curriculumIds) {
		validatePracticeSettings(command.dailyMinutes(), command.daysPerWeek());
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
		String title = requireNonNull(command.dogName()) + TITLE_SUFFIX;
		PlanCreateCommand createCommand = new PlanCreateCommand(command.userId(), command.dogId(), title,
				CUSTOM_GOAL, command.dailyMinutes(), command.daysPerWeek());
		return create(createCommand, curriculumIds);
	}

	public static Plan createAi(AiPlanCreateCommand command, List<Long> curriculumIds) {
		String title = requireNonNull(command.dogName()) + TITLE_SUFFIX;
		PlanCreateCommand createCommand = new PlanCreateCommand(command.userId(), command.dogId(), title,
				command.goal(), command.dailyMinutes(), command.daysPerWeek());

		Plan plan = create(createCommand, curriculumIds);
		plan.expectation = requireNonNull(command.expectation());
		return plan;
	}

	public List<Long> getCurriculumIds() {
		return Collections.unmodifiableList(curriculumIds);
	}

	public Set<Long> getCompletedLessonIds() {
		return Collections.unmodifiableSet(completedLessonIds);
	}

	public void completeLesson(Long lessonId) {
		state(status == PlanStatus.ACTIVE, "활성 플랜만 레슨을 완료할 수 있습니다.");
		completedLessonIds.add(requireNonNull(lessonId));
	}

	public Optional<Long> findCurrentLessonId(List<Long> orderedLessonIds) {
		state(!orderedLessonIds.isEmpty(), "플랜에는 레슨이 하나 이상 있어야 합니다.");
		return orderedLessonIds.stream()
				.filter(lessonId -> !completedLessonIds.contains(lessonId))
				.findFirst();
	}

	public boolean hasCompletedAllLessons(List<Long> currentLessonIds) {
		state(!currentLessonIds.isEmpty(), "플랜에는 레슨이 하나 이상 있어야 합니다.");
		return completedLessonIds.containsAll(currentLessonIds);
	}

	public void complete(Instant completedAt) {
		state(status == PlanStatus.ACTIVE, "활성 플랜만 완료할 수 있습니다.");
		this.completedAt = requireNonNull(completedAt);
		status = PlanStatus.COMPLETED;
	}

	public void replace() {
		state(status == PlanStatus.ACTIVE, "활성 플랜만 교체할 수 있습니다.");
		status = PlanStatus.REPLACED;
	}

	public static void validatePracticeSettings(int dailyMinutes, int daysPerWeek) {
		validateDailyMinutes(dailyMinutes);
		validateDaysPerWeek(daysPerWeek);
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
