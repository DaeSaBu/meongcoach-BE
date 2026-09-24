package com.daesabu.meongcoach.payment.domain;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스토어 구매 한 건과 그로 얻은 이용권. 한 행이 한 거래이며, 어느 시기를 열어 주는지는 저장하지 않고 상품 구성({@link ProductId})에서 계산한다.
 * 그래서 상품 구성을 바꾸면 기존 구매에도 소급 적용된다.
 * 바뀌는 값은 회수 시각뿐이라 수정 시각 없이 생성 시각만 기록한다.
 */
@Getter
@Entity
@Table(
		name = "entitlements",
		uniqueConstraints = @UniqueConstraint(columnNames = "transaction_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entitlement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 스토어 거래 ID(웹훅 transaction_id). 부여·회수를 거래 단위로 해서 웹훅 재전송과 환불 후 재구매를 구분한다
	@Column(name = "transaction_id", nullable = false, length = 100)
	private String transactionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProductId productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Store store;

	// 결제 통화 기준 가격(웹훅 price_in_purchased_currency). 스토어가 알려 주지 않으면 null
	@Column(precision = 19, scale = 4)
	private BigDecimal price;

	// ISO 4217 통화 코드(웹훅 currency). 스토어가 알려 주지 않으면 null
	@Column(length = 3)
	private String currency;

	// 스토어에서 구매한 시각(웹훅 purchased_at_ms). 행을 기록한 시각은 createdAt이다
	@Column(nullable = false)
	private Instant purchasedAt;

	// 회수되지 않은 이용권은 null
	private Instant revokedAt;

	// 같은 거래의 중복 부여를 Entitlements.grant()가 막으므로 같은 패키지에서만 연다
	static Entitlement grant(EntitlementRegisterCommand command) {
		Entitlement entitlement = new Entitlement();

		entitlement.userId = Objects.requireNonNull(command.userId());
		entitlement.transactionId = Objects.requireNonNull(command.transactionId());
		entitlement.productId = Objects.requireNonNull(command.productId());
		entitlement.store = Objects.requireNonNull(command.store());
		entitlement.price = command.price();
		entitlement.currency = command.currency();
		entitlement.purchasedAt = Objects.requireNonNull(command.purchasedAt());

		return entitlement;
	}

	// 거래 단위 회수 규칙은 Entitlements.revoke()에 있으므로 application이 우회하지 못하게 같은 패키지에서만 연다
	void revoke(Instant revokedAt) {
		if (!isActive()) {
			return;
		}
		this.revokedAt = Objects.requireNonNull(revokedAt);
	}

	public boolean grants(LifeStage lifeStage) {
		return isActive() && productId.grants(lifeStage);
	}

	public boolean isActive() {
		return revokedAt == null;
	}
}
