package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(length = 512)
	private String profileImageUrl;

	// 스킵해도 완료로 기록한다 (U-0104)
	private Boolean isCompletedTooltip;

	// 스킵해도 완료로 기록한다 (U-0105)
	private Boolean isCompletedOnboarding;

	public static UserProfile create(User user, String nickname) {
		UserProfile profile = new UserProfile();
		profile.user = user;
		profile.nickname = nickname;
		profile.isCompletedTooltip = false;
		profile.isCompletedOnboarding = false;
		return profile;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changeProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void completeTooltip() {
		this.isCompletedTooltip = true;
	}

	public void completeOnboarding() {
		this.isCompletedOnboarding = true;
	}
}
