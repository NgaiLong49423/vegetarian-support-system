-- Local-only fixture: give seeded Expert accounts a synthetic promotion history.
-- Existing application approvals and role-change history remain authoritative.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @administrator_id BIGINT;
SELECT @administrator_id = user_id
FROM [USER]
WHERE email = 'demo-admin@mamxanh.local' AND role = 'ADMIN';

IF @administrator_id IS NULL
    THROW 51090, 'The demo administrator is required for expert promotion fixtures.', 1;

INSERT INTO [USER_ACCOUNT_AUDIT]
    (target_user_id, administrator_id, action, old_role, new_role, reason, created_at)
SELECT u.user_id, @administrator_id, 'ROLE_CHANGED', 'CUSTOMER', 'EXPERT',
       N'Local demo fixture: simulated Expert promotion.', u.created_at
FROM [USER] u
WHERE u.role = 'EXPERT'
  AND (u.email IN ('demo-expert@mamxanh.local', 'demo-expert-02@mamxanh.local',
                   'demo-expert-03@mamxanh.local', 'demo-expert-04@mamxanh.local',
                   'demo-customer@mamxanh.local', 'demo-new-member@mamxanh.local',
                   'demo-applicant@mamxanh.local')
       OR u.email LIKE 'demo-customer-[0-9][0-9]@mamxanh.local')
  AND NOT EXISTS (
      SELECT 1 FROM [EXPERT_APPLICATION] a
      WHERE a.user_id = u.user_id AND a.status = 'APPROVED' AND a.reviewed_at IS NOT NULL
  )
  AND NOT EXISTS (
      SELECT 1 FROM [USER_ACCOUNT_AUDIT] audit
      WHERE audit.target_user_id = u.user_id
        AND audit.action = 'ROLE_CHANGED' AND audit.new_role = 'EXPERT'
  );

COMMIT TRANSACTION;
