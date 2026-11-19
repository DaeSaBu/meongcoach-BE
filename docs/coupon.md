# 쿠폰

코드를 입력한 사용자에게 AI 행동 분석 횟수를 지급합니다. 코드와 사용 기록은 `coupon` 모듈이 갖고, 횟수는 `analysis` 모듈의 `ai_trials`가 갖습니다. ([ai-pipeline.md](ai-pipeline.md)의 무료 체험 횟수 항목)

## 쿠폰 적재

- **쿠폰은 생성 API 없이 SQL로 넣습니다.** 그래서 `Coupon`에는 정적 팩토리가 없고, 테스트는 `CouponFixture`로 만듭니다.

  ```sql
  INSERT INTO coupons (code, ai_trial_count, redeemable_until)
  VALUES ('KINTEX', 10, '2026-12-01T00:00:00+09:00');
  ```

- **코드는 대문자로 저장합니다.** 입력은 앞뒤 공백을 지우고 대문자로 바꿔 대조하므로(`Coupon.normalizeCode`), 소문자로 저장한 코드는 영영 일치하지 않습니다. 이를 막으려고 `chk_coupons_code_uppercase` 제약이 소문자·앞뒤 공백·빈 코드의 INSERT를 거절합니다.
- **`redeemable_until`은 그 시각을 포함하지 않습니다.** 그 시각부터 `COUPON_EXPIRED`이고, `NULL`이면 기한 없이 사용할 수 있습니다.
- **사용 인원 제한은 없습니다.** 공용 코드라 코드가 퍼지면 기한까지 누구나 사용할 수 있습니다. 일찍 닫으려면 `redeemable_until`을 당깁니다. 선착순·1회용 코드가 필요해지면 최대 사용 횟수 컬럼과 쿠폰 행 잠금을 새로 설계해야 합니다.

## 사용

- **한 사용자는 같은 쿠폰을 한 번만 씁니다.** `CouponRedemptionModifyService`가 회원 ID로 advisory lock을 잡은 뒤 사용 기록을 확인하므로, 같은 사용자의 요청이 겹쳐도 두 번째 요청은 `COUPON_ALREADY_REDEEMED`로 끝납니다. `uk_coupon_redemptions_coupon_id_user_id`는 락을 거치지 않는 경로에 대비한 안전망이라 위반을 도메인 예외로 바꾸지 않습니다.
- **락 키는 회원 ID 그대로이고 `analysis`의 락과 같은 키입니다.** 쿠폰 사용 트랜잭션 안에서 `AiTrialRegister.grant`가 같은 락을 다시 잡지만, 같은 트랜잭션의 advisory lock은 재진입되어 기다리지 않습니다. 분석 완료(`AiAnalysisModifyService.complete`)와 겹치면 서로 끝날 때까지 기다립니다.
- **지급한 횟수는 회수하지 않습니다.** 사용 기록이나 쿠폰 행을 지워도 `ai_trials.granted_count`는 그대로입니다. 사용 기록을 지우면 그 사용자는 같은 쿠폰을 다시 쓸 수 있으므로, 회수가 필요하면 `granted_count`를 직접 내립니다.
- **사용 기록은 지급 당시 횟수를 따로 저장합니다** (`coupon_redemptions.ai_trial_count`). 쿠폰 행의 횟수를 나중에 바꿔도 이미 지급한 횟수의 기록은 바뀌지 않습니다.
- **온보딩을 마치지 않은 회원도 사용할 수 있습니다.** 온보딩 화면에서 코드를 입력하기 때문이고, 횟수는 회원 ID에 붙으므로 강아지 프로필이 없어도 지급됩니다. ([security.md](security.md)의 URL 인가 규칙)
- **코드 추측 시도는 제한하지 않습니다.** 없는 코드는 횟수 제한 없이 404로 끝납니다. 짧거나 뻔한 코드를 쓰면 추측될 수 있습니다.

## RevenueCat에 두지 않은 이유

- RevenueCat이 다루는 코드는 스토어가 발급하는 offer code·promo code이고 스토어 상품(구독 등)에만 적용됩니다. 임의 코드를 대조하는 기능은 없습니다.
- 이용권(entitlement)은 만료 시각이 있는 접근 권한이라 "10회" 같은 횟수를 담지 못합니다.
- 횟수를 담을 수 있는 In-App Currency는 잔액 체계가 이미 백엔드에 있으면 백엔드를 원천으로 두라고 안내합니다. 이 저장소는 분석 완료 시 `ai_trials`에서 차감합니다.
- 인앱 결제로 산 횟수는 In-App Currency 잔액으로 쌓이고, 백엔드는 그 잔액을 `ai_trials.purchased_count`로 옮겨 적습니다. 쿠폰 지급분(`granted_count`)과 컬럼이 달라 동기화가 쿠폰 지급분을 덮어쓰지 않습니다. ([ai-pipeline.md](ai-pipeline.md)의 구매 횟수 항목)
