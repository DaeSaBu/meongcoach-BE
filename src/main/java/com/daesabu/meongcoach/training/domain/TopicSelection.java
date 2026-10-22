package com.daesabu.meongcoach.training.domain;

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
		name = "topic_selection",
		uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicSelection extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long topicId;

	public static TopicSelection enter(Long userId, Long topicId) {
		TopicSelection topicSelection = new TopicSelection();

		topicSelection.userId = requireNonNull(userId);
		topicSelection.topicId = requireNonNull(topicId);

		return topicSelection;
	}

	public void moveTo(Long topicId) {
		this.topicId = topicId;
	}
}
