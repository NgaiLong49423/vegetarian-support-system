-- Distinguish user-created standalone checklists from meal-plan and recipe lists.
ALTER TABLE [SHOPPING_LIST] DROP CONSTRAINT [CK_SHOPPING_LIST_kind];

ALTER TABLE [SHOPPING_LIST] ADD CONSTRAINT [CK_SHOPPING_LIST_kind]
    CHECK (list_kind IN ('MEAL_PLAN', 'PERSONAL', 'MANUAL'));
