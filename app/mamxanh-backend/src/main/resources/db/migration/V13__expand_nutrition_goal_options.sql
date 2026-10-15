-- FR-35/FR-36: allow a nullable general goal and explicit weight/muscle goals.
ALTER TABLE [USER] DROP CONSTRAINT CK_USER_nutrition_goal;

ALTER TABLE [USER] ADD CONSTRAINT CK_USER_nutrition_goal CHECK (
    nutrition_goal IS NULL OR nutrition_goal IN (
        'MAINTAIN_WEIGHT', 'LOSE_WEIGHT', 'GAIN_WEIGHT', 'BUILD_MUSCLE',
        'IMPROVE_HEALTH', 'SUPPORT_TRAINING'
    )
);
