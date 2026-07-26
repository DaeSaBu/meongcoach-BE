package com.daesabu.meongcoach.user.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "social_accounts",
		uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialProvider provider;

	@Column(nullable = false, length = 255)
	private String providerId;

	// 소셜 제공자가 이메일을 내려주지 않을 수 있으므로 nullable
	@Column(length = 255)
	private String email;

	private SocialAccount(User user, SocialAccountLinkCommand command) {
		this.user = user;
		this.provider = command.provider();
		this.providerId = command.providerId();
		this.email = command.email();
	}

	public static SocialAccount link(User user, SocialAccountLinkCommand command) {
		return new SocialAccount(user, command);
	}
}
