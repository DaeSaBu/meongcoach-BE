-- 이용권 식별자를 RevenueCat 원본 문자열 대신 EntitlementType 상수 이름(PUPPY 등)으로 저장하면서 컬럼 이름을 type으로 바꾼다.
-- V6 이후 저장된 행이 없어 값 변환은 하지 않는다.
ALTER TABLE "entitlements" RENAME COLUMN "identifier" TO "type";
ALTER TABLE "entitlements" RENAME CONSTRAINT "entitlements_purchase_id_identifier_key" TO "entitlements_purchase_id_type_key";

-- 엔티티 @Index와 이름·컬럼을 맞춘다.
ALTER INDEX "idx_entitlements_user_id_identifier" RENAME TO "idx_entitlements_user_id_type";
