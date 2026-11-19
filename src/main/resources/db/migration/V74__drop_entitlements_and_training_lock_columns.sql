DROP TABLE entitlements;

ALTER TABLE training_categories DROP COLUMN required_entitlement_type;

ALTER TABLE curriculums DROP COLUMN is_premium;
