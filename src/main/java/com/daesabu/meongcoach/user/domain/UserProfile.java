package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import com.daesabu.meongcoach.user.domain.command.UserProfileCreateCommand;

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
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 프로필. 온보딩은 스킵할 수 없으므로 온보딩 완료 시점에 생성되며,
 * 온보딩 완료 여부는 별도 플래그 없이 프로필 행 존재 여부로 판단한다.
 */
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

	// 온보딩 이후 선택 입력 — 미설정은 빈 문자열로 저장한다
	@Column(nullable = false, length = 512)
	private String profileImageUrl;

	// 미입력(나이 미상)을 허용하므로 nullable — getAge()가 null 반환. 타임존 무관 — LocalDate 유지
	private LocalDate birthDate;

	// 온보딩 필수 입력이며 16가지 유형만 저장한다
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 4)
	private Mbti mbti;

	// 온보딩 필수 입력이며 응답하지 않음은 null 대신 NONE으로 저장한다
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Gender gender;

	@Column(name = "prior_training_topic_ids", nullable = false)
	private Set<Long> priorTrainingTopicIds = new HashSet<>();

	@Column(name = "training_goal_topic_ids", nullable = false)
	private Set<Long> trainingGoalTopicIds = new HashSet<>();

	// 스킵해도 완료로 기록한다 (U-0104)
	@Column(nullable = false)
	private Boolean isCompletedTooltip;

	private UserProfile(User user, UserProfileCreateCommand command) {
		this.user = user;
		this.nickname = command.nickname();
		this.profileImageUrl = Objects.requireNonNullElse(command.profileImageUrl(), "");
		this.birthDate = command.birthDate();
		this.mbti = Mbti.from(command.mbti());
		this.gender = Gender.from(command.gender());
		this.priorTrainingTopicIds = copyOrEmpty(command.priorTrainingTopicIds());
		this.trainingGoalTopicIds = copyOrEmpty(command.trainingGoalTopicIds());
		this.isCompletedTooltip = false;
	}

	public static UserProfile create(User user, UserProfileCreateCommand command) {
		return new UserProfile(user, command);
	}

	// 토픽 미선택은 null로 들어올 수 있으므로 빈 집합으로 취급한다
	private static Set<Long> copyOrEmpty(Set<Long> topicIds) {
		if (topicIds == null) {
			return new HashSet<>();
		}
		return new HashSet<>(topicIds);
	}

	public Integer getAge() {
		if (birthDate == null) {
			return null;
		}
		return Period.between(birthDate, LocalDate.now()).getYears();
	}

	public void completeTooltip() {
		this.isCompletedTooltip = true;
	}
}
