package com.daesabu.meongcoach.auth.domain;

import com.daesabu.meongcoach.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
		name = "social_accounts",
		uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SocialProvider provider;

	@Column(nullable = false, length = 255)
	private String providerId;

	@Embedded
	private Email email;

	private SocialAccount(Long userId, SocialAccountLinkCommand command) {
		this.userId = userId;
		this.provider = command.provider();
		this.providerId = command.providerId();
		this.email = command.email();
	}

	public static SocialAccount link(Long userId, SocialAccountLinkCommand command) {
		return new SocialAccount(userId, command);
	}
}
