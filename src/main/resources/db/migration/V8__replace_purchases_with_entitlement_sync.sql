-- 이용권을 구매 기록에서 만들지 않고 RevenueCat이 계산한 회원의 활성 이용권을 그대로 옮겨 두는 사본으로 바꾼다.
-- 한 행이 회원·종류 하나라 (user_id, type)이 유니크다. 환불로 회수된 뒤 다시 활성이 되면 새 행 대신 revoked_at을 비운다.

-- 구매 단위로만 중복을 막던 동안 같은 회원·종류가 여러 행 생겼을 수 있다. 가장 먼저 만든 행만 남긴다
DELETE FROM "entitlements" e
    USING "entitlements" d
WHERE e."user_id" = d."user_id"
  AND e."type" = d."type"
  AND e."id" > d."id";

-- 이 컬럼을 쓰는 외래 키와 (purchase_id, type) 유니크 제약도 함께 지워진다
ALTER TABLE "entitlements" DROP COLUMN "purchase_id";

-- 회원이 특정 이용권을 가졌는지 확인하는 조회는 아래 유니크 제약의 인덱스가 맡는다. 엔티티 @UniqueConstraint와 이름을 맞춘다
DROP INDEX "idx_entitlements_user_id_type";
ALTER TABLE "entitlements" ADD CONSTRAINT "uk_entitlements_user_id_type" UNIQUE ("user_id", "type");

-- 구매 기록은 읽는 곳이 없고 금액·스토어·구매 시각은 RevenueCat 대시보드와 스토어 리포트가 원천이라 지운다
DROP TABLE "purchases";
