package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	public static UserProfile create(UserProfileCreateCommand command) {
		UserProfile profile = new UserProfile();
		profile.nickname = command.nickname();
		profile.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		profile.mbti = Mbti.from(command.mbti());
		profile.priorTrainingTopicIds = copyOrEmpty(command.priorTrainingTopicIds());
		profile.trainingGoalTopicIds = copyOrEmpty(command.trainingGoalTopicIds());
		profile.isCompletedTooltip = false;
		return profile;
	}

	private static Set<Long> copyOrEmpty(Set<Long> topicIds) {
		if (topicIds == null) {
			return new HashSet<>();
		}
		return new HashSet<>(topicIds);
	}

	public void completeTooltip() {
		this.isCompletedTooltip = true;
	}
}
