package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

	@Id
	private Long userId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(nullable = false, length = 512)
	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(length = 4)
	private Mbti mbti;

	@Column(name = "prior_training_topic_ids", nullable = false)
	private Set<Long> priorTrainingTopicIds = new HashSet<>();

	@Column(name = "training_goal_topic_ids", nullable = false)
	private Set<Long> trainingGoalTopicIds = new HashSet<>();

	@Column(nullable = false)
	private Boolean isCompletedTooltip;

	private UserProfile(User user, UserProfileCreateCommand command) {
		this.user = user;
		this.nickname = command.nickname();
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.mbti = parseMbti(command.mbti());
		this.priorTrainingTopicIds = copyOrEmpty(command.priorTrainingTopicIds());
		this.trainingGoalTopicIds = copyOrEmpty(command.trainingGoalTopicIds());
		this.isCompletedTooltip = false;
	}

	public static UserProfile create(User user, UserProfileCreateCommand command) {
		return new UserProfile(user, command);
	}

	private static Mbti parseMbti(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Mbti.from(value);
	}

	private static Set<Long> copyOrEmpty(Set<Long> topicIds) {
		if (topicIds == null) {
			return new HashSet<>();
		}
		return new HashSet<>(topicIds);
	}

	public void updateProfile(UserProfileUpdateCommand command) {
		Mbti newMbti = parseMbti(command.mbti());
		this.nickname = command.nickname();
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.mbti = newMbti;
	}

	public void addPriorTrainingTopics(Collection<Long> topicIds) {
		priorTrainingTopicIds.addAll(topicIds);
	}

	public void completeTooltip() {
		this.isCompletedTooltip = true;
	}
}
