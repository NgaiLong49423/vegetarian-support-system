ALTER TABLE AI_CREDIT_LEDGER
    ADD system_absorbed_credits DECIMAL(38,9) NULL;

EXEC(N'ALTER TABLE AI_CREDIT_LEDGER
    ADD CONSTRAINT CK_AI_CREDIT_LEDGER_absorbed_credits CHECK (
        system_absorbed_credits IS NULL OR system_absorbed_credits >= 0)');
