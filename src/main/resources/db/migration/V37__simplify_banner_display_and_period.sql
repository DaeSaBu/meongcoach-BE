ALTER TABLE banners DROP CONSTRAINT chk_banners_benefit_fields;
ALTER TABLE banners DROP CONSTRAINT chk_banners_period;

ALTER TABLE banners RENAME COLUMN starts_on TO starts_at;
ALTER TABLE banners RENAME COLUMN ends_on TO expires_at;
ALTER TABLE banners ALTER COLUMN starts_at TYPE TIMESTAMP WITH TIME ZONE
    USING starts_at::timestamp AT TIME ZONE 'Asia/Seoul';
ALTER TABLE banners ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE
    USING (expires_at + 1)::timestamp AT TIME ZONE 'Asia/Seoul';

ALTER TABLE banners DROP COLUMN disclosure;
ALTER TABLE banners DROP COLUMN validity_label;
ALTER TABLE banners DROP COLUMN image_background;

ALTER TABLE banners ADD CONSTRAINT chk_banners_benefit_fields CHECK (
    type <> 'BENEFIT'
    OR (benefit IS NOT NULL AND place IS NOT NULL AND starts_at IS NOT NULL AND expires_at IS NOT NULL)
);
ALTER TABLE banners ADD CONSTRAINT chk_banners_period CHECK (
    starts_at IS NULL OR expires_at IS NULL OR starts_at < expires_at
);
