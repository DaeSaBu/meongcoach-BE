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
import java.time.LocalDate;
import java.time.Period;
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
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(length = 512)
	private String profileImageUrl;

	// 타임존 무관 — LocalDate 유지, 나이는 저장하지 않고 계산으로 파생한다
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(length = 4)
	private Mbti mbti;

	// 스킵해도 완료로 기록한다 (U-0104)
	private Boolean isCompletedTooltip;

	public static UserProfile create(User user, String nickname) {
		UserProfile profile = new UserProfile();
		profile.user = user;
		profile.nickname = nickname;
		profile.isCompletedTooltip = false;
		return profile;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changeProfileImage(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public void changeBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public void changeMbti(Mbti mbti) {
		this.mbti = mbti;
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
