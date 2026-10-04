package com.daesabu.meongcoach.progress.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "topic_progress",
		uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicProgress extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long topicId;

	public static TopicProgress enter(Long userId, Long topicId) {
		TopicProgress topicProgress = new TopicProgress();

		topicProgress.userId = requireNonNull(userId);
		topicProgress.topicId = requireNonNull(topicId);

		return topicProgress;
	}

	public void moveTo(Long topicId) {
		this.topicId = topicId;
	}
}
