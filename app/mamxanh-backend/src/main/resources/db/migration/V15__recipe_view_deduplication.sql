-- FR-58 / Issue #57: last-event lookups and repair the existing cached counter.
CREATE NONCLUSTERED INDEX IX_RECIPE_VIEW_member_recipe_time
    ON RECIPE_VIEW (recipe_id, user_id, viewed_at DESC)
    WHERE user_id IS NOT NULL;

CREATE NONCLUSTERED INDEX IX_RECIPE_VIEW_guest_recipe_time
    ON RECIPE_VIEW (recipe_id, anonymous_viewer_hash, viewed_at DESC)
    WHERE user_id IS NULL AND anonymous_viewer_hash IS NOT NULL;

UPDATE rp SET view_count = (SELECT COUNT(*) FROM RECIPE_VIEW rv WHERE rv.recipe_id = rp.recipe_id)
FROM RECIPE_POST rp;
