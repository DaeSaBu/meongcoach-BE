package com.daesabu.meongcoach.community.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.community.domain.exception.SelfBlockNotAllowedException;
import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
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
		name = "user_blocks",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_user_blocks_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long blockerId;

	@Column(nullable = false)
	private Long blockedId;

	public static UserBlock register(Long blockerId, Long blockedId) {
		requireNonNull(blockerId);
		requireNonNull(blockedId);
		if (blockerId.equals(blockedId)) {
			throw new SelfBlockNotAllowedException();
		}
		UserBlock block = new UserBlock();

		block.blockerId = blockerId;
		block.blockedId = blockedId;

		return block;
	}
}
