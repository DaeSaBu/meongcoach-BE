-- 구매를 RevenueCat REST API v2로 조회하면서 product_id가 스토어 상품 ID가 아닌 RevenueCat 내부 ID로 바뀐다.
-- 무엇을 샀는지는 이 구매로 부여한 entitlements.type으로 남으므로 컬럼을 지운다.
ALTER TABLE "purchases" DROP COLUMN "product_id";
