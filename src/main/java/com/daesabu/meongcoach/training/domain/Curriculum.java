package com.daesabu.meongcoach.training.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "curriculums")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curriculum extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false)
	private int sortOrder;

	@Column(nullable = false, length = 512)
	private String thumbnailUrl;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private boolean isPremium;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = false)
	private Topic topic;

	@OneToMany(mappedBy = "curriculum")
	@OrderBy("sortOrder ASC, id ASC")
	private List<Lesson> lessons = new ArrayList<>();

	public List<Long> getLessonIds() {
		return lessons.stream()
				.map(Lesson::getId)
				.toList();
	}

	public int getLessonsSize() {
		return lessons.size();
	}

	public CurriculumStatus statusOf(Set<Long> completedLessonIds) {
		int totalLessons = getLessonsSize();
		int completedLessons = countCompletedLessons(completedLessonIds);
		return CurriculumStatus.of(totalLessons, completedLessons);
	}

	public int countCompletedLessons(Set<Long> completedLessonIds) {
		return (int) lessons.stream()
				.map(Lesson::getId)
				.filter(completedLessonIds::contains)
				.count();
	}
}
