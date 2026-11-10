-- All enabled models are available to every member tier. Free access flags
-- continue to control Guest access; the common default remains Guest-eligible.
UPDATE AI_GEMINI_MODEL_CONFIGURATION
SET plus_allowed = enabled,
    pro_allowed = enabled,
    plus_default = free_default,
    pro_default = free_default,
    updated_at = SYSUTCDATETIME();
