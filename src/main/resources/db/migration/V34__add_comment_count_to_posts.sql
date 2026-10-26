ALTER TABLE "posts" ADD COLUMN "comment_count" INTEGER NOT NULL DEFAULT 0;

UPDATE "comments" r
SET "deleted_at" = root."deleted_at"
FROM "comment_closures" cc
JOIN "comments" root ON root."id" = cc."ancestor_id"
WHERE cc."descendant_id" = r."id"
  AND cc."depth" = 1
  AND root."deleted_at" IS NOT NULL
  AND r."deleted_at" IS NULL;

UPDATE "posts" p
SET "comment_count" = (
    SELECT COUNT(*)
    FROM "comments" c
    WHERE c."post_id" = p."id"
      AND c."deleted_at" IS NULL
);
