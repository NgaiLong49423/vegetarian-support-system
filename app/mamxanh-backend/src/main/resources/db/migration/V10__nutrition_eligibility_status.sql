-- FR-38: store the current eligibility state separately from FR-35 health-data consent.
ALTER TABLE [USER]
    ADD nutrition_eligibility_status VARCHAR(20) NOT NULL
        CONSTRAINT DF_USER_nutrition_eligibility_status DEFAULT 'NOT_CONFIRMED',
        nutrition_eligibility_confirmed_at DATETIME2(7) NULL;
GO

ALTER TABLE [USER]
    ADD CONSTRAINT CK_USER_nutrition_eligibility_status
        CHECK (nutrition_eligibility_status IN ('NOT_CONFIRMED', 'ELIGIBLE', 'INELIGIBLE'));
GO

ALTER TABLE [USER]
    ADD CONSTRAINT CK_USER_nutrition_eligibility_timestamp CHECK (
        (nutrition_eligibility_status = 'NOT_CONFIRMED' AND nutrition_eligibility_confirmed_at IS NULL)
        OR (nutrition_eligibility_status IN ('ELIGIBLE', 'INELIGIBLE') AND nutrition_eligibility_confirmed_at IS NOT NULL)
    );
