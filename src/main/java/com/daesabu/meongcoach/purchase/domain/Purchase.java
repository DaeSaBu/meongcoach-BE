package com.daesabu.meongcoach.purchase.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 스토어 구매 한 건. 결제 이력과 환불 판단의 기준이며, 이 구매로 얻은 권한은 entitlement 모듈이 따로 가진다.
 * 무엇을 샀는지는 이 구매로 부여한 이용권 종류로 남으므로 상품 ID는 저장하지 않는다. 스토어는 판매 경로가 정해져 있어 Store로 저장한다.
 * 바뀌는 값은 환불 시각뿐이라 수정 시각 없이 생성 시각만 기록한다.
 */
@Getter
@Entity
@Table(
		name = "purchases",
		uniqueConstraints = @UniqueConstraint(columnNames = "transaction_id"),
		// 결제 이력을 회원별로 구매 시각 순으로 조회한다. V6 마이그레이션과 이름·컬럼을 맞춘다
		indexes = @Index(name = "idx_purchases_user_id_purchased_at", columnList = "user_id, purchased_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Purchase extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 회원은 user 모듈의 애그리거트라 연관 대신 ID로만 참조한다
	@Column(name = "user_id", nullable = false)
	private Long userId;

	// 스토어 거래 ID(RevenueCat store_purchase_identifier). 반복 동기화와 환불 후 재구매를 거래 단위로 구분한다
	@Column(name = "transaction_id", nullable = false, length = 100)
	private String transactionId;

	// 구매가 일어난 스토어
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private Store store;

	// RevenueCat이 USD로 환산한 매출(revenue_in_usd.gross, 세금·수수료 차감 전). 결제 통화 기준 가격이 아니다
	@Column(precision = 19, scale = 4)
	private BigDecimal price;

	// price의 ISO 4217 통화 코드(revenue_in_usd.currency). RevenueCat 응답상 USD다
	@Column(length = 3)
	private String currency;

	// 스토어에서 구매한 시각. 행을 기록한 시각은 createdAt이다
	@Column(name = "purchased_at", nullable = false)
	private Instant purchasedAt;

	// 환불되지 않은 구매는 null
	private Instant refundedAt;

	public static Purchase register(Long userId, PurchaseRegisterCommand command) {
		Purchase purchase = new Purchase();

		purchase.userId = requireNonNull(userId);
		purchase.transactionId = requireNonNull(command.transactionId());
		purchase.store = requireNonNull(command.store());
		purchase.price = command.price();
		purchase.currency = command.currency();
		purchase.purchasedAt = requireNonNull(command.purchasedAt());

		return purchase;
	}
}
