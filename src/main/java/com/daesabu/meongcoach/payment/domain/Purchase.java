package com.daesabu.meongcoach.payment.domain;

import static java.util.Objects.requireNonNull;

import com.daesabu.meongcoach.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 스토어 구매 한 건. 결제 이력과 환불 판단의 기준이며, 이 구매로 얻은 권한은 {@link Entitlement}가 따로 가진다.
 * 상품·스토어는 RevenueCat이 정의하는 값이라 enum으로 복제하지 않고 웹훅 값을 그대로 저장한다.
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

	// 스토어 거래 ID(웹훅 transaction_id). 웹훅 재전송과 환불 후 재구매를 거래 단위로 구분한다
	@Column(name = "transaction_id", nullable = false, length = 100)
	private String transactionId;

	// RevenueCat 상품 식별자(웹훅 product_id) 원본
	@Column(nullable = false, length = 100)
	private String productId;

	// 구매가 일어난 스토어(웹훅 store) 원본. APP_STORE·PLAY_STORE·PROMOTIONAL 등
	@Column(nullable = false, length = 30)
	private String store;

	// 결제 통화 기준 가격(웹훅 price_in_purchased_currency). 스토어가 알려 주지 않으면 null
	@Column(precision = 19, scale = 4)
	private BigDecimal price;

	// ISO 4217 통화 코드(웹훅 currency). 스토어가 알려 주지 않으면 null
	@Column(length = 3)
	private String currency;

	// 스토어에서 구매한 시각(웹훅 purchased_at_ms). 행을 기록한 시각은 createdAt이다
	@Column(name = "purchased_at", nullable = false)
	private Instant purchasedAt;

	// 환불되지 않은 구매는 null
	private Instant refundedAt;

	public static Purchase register(Long userId, PurchaseRegisterCommand command) {
		Purchase purchase = new Purchase();

		purchase.userId = requireNonNull(userId);
		purchase.transactionId = requireNonNull(command.transactionId());
		purchase.productId = requireNonNull(command.productId());
		purchase.store = requireNonNull(command.store());
		purchase.price = command.price();
		purchase.currency = command.currency();
		purchase.purchasedAt = requireNonNull(command.purchasedAt());

		return purchase;
	}
}
