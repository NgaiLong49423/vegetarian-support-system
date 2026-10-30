-- Repeatable demo seed: 38 AI usage telemetry calls across FR-21, FR-34, FR-36, FR-37, FR-47, FR-51
-- Never loaded outside profile local.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @expert_id BIGINT;
DECLARE @customer_id BIGINT;
DECLARE @new_member_id BIGINT;
DECLARE @customer_04_id BIGINT;
DECLARE @customer_05_id BIGINT;
DECLARE @customer_06_id BIGINT;

SELECT @expert_id = user_id FROM [USER] WHERE email = 'demo-expert@mamxanh.local';
SELECT @customer_id = user_id FROM [USER] WHERE email = 'demo-customer@mamxanh.local';
SELECT @new_member_id = user_id FROM [USER] WHERE email = 'demo-new-member@mamxanh.local';
SELECT @customer_04_id = user_id FROM [USER] WHERE email = 'demo-customer-04@mamxanh.local';
SELECT @customer_05_id = user_id FROM [USER] WHERE email = 'demo-customer-05@mamxanh.local';
SELECT @customer_06_id = user_id FROM [USER] WHERE email = 'demo-customer-06@mamxanh.local';

IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '445638cf-acce-532f-9938-cc512f16529f')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '445638cf-acce-532f-9938-cc512f16529f', @customer_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 01:15:00', 420, 280, 770, 70,
        50, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001593750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '52163438-8f8e-5e9e-ad83-7961faaabe68')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '52163438-8f8e-5e9e-ad83-7961faaabe68', NULL, 'f344b718-3209-5243-abf2-23299d228647', 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 01:45:00', 350, 220, 620, 50,
        0, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001275000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'b414dc40-aed9-5108-a2a4-e6f5776abf0e')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'b414dc40-aed9-5108-a2a4-e6f5776abf0e', @customer_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 02:10:00', 510, 310, 900, 80,
        80, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001791000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '5ea8f7d8-8c75-500d-9acf-be23c9f43022')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '5ea8f7d8-8c75-500d-9acf-be23c9f43022', NULL, 'fed10f29-2895-5025-98ac-24e9ef14bcb0', 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 03:00:00', 380, 240, 680, 60,
        0, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001410000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '17595f3f-066c-5ba0-b113-5e2842be173c')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '17595f3f-066c-5ba0-b113-5e2842be173c', @new_member_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 04:20:00', 460, 290, 825, 75,
        60, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001673250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '09aada45-0961-566f-bd69-b5923a8f0c7d')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '09aada45-0961-566f-bd69-b5923a8f0c7d', @customer_06_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 05:10:00', 620, 380, 1090, 90,
        120, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002146500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'defa91aa-ee79-5532-b85c-3dc2033457ee')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'defa91aa-ee79-5532-b85c-3dc2033457ee', NULL, '4e201695-54b4-5a7a-a339-bde2c6679e85', 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 06:30:00', 410, 260, 735, 65,
        0, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001526250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'ab939ee7-2d9d-51e7-8ec6-ed0860d9f258')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'ab939ee7-2d9d-51e7-8ec6-ed0860d9f258', @customer_04_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 07:15:00', 480, 300, 860, 80,
        70, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001737750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'c0faf13a-8c1f-523e-a830-c1a0f0657cd8')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'c0faf13a-8c1f-523e-a830-c1a0f0657cd8', @customer_05_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 08:05:00', 530, 320, 935, 85,
        90, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001855500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'ba214149-a79d-55c3-8129-47139168e0cd')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'ba214149-a79d-55c3-8129-47139168e0cd', NULL, 'f275812d-c73c-5331-936a-a01c50d97f96', 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 09:00:00', 390, 250, 700, 60,
        0, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001455000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '4a4dda68-6194-58bd-a917-91ac299e81f5')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '4a4dda68-6194-58bd-a917-91ac299e81f5', @expert_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 10:15:00', 680, 420, 1210, 110,
        150, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002396250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '1d4e9c99-a686-508f-b904-54f7dbb9d769')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '1d4e9c99-a686-508f-b904-54f7dbb9d769', @customer_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 11:20:00', 560, 340, 990, 90,
        100, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001965000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'a6f36949-53b2-54e7-806b-7d4939b66741')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'a6f36949-53b2-54e7-806b-7d4939b66741', @customer_06_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 12:40:00', 590, 360, 1045, 95,
        110, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002074500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'c31f995a-07ab-5330-9072-0e5314e134f7')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'c31f995a-07ab-5330-9072-0e5314e134f7', @customer_id, NULL, 'FR-51', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 14:35:00', 640, 390, 1130, 100,
        130, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002229750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'a8c39ee9-cfdb-5ff8-966f-a7a26420c533')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'a8c39ee9-cfdb-5ff8-966f-a7a26420c533', @customer_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 02:30:00', 850, 480, 1450, 120,
        150, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002786250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '5e836cbd-5d95-5ed3-ba8b-b867cbb0f632')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '5e836cbd-5d95-5ed3-ba8b-b867cbb0f632', @customer_06_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 04:50:00', 1020, 560, 1720, 140,
        220, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003241500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '2475527d-f5ec-5ef1-b51d-4158c314fad8')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '2475527d-f5ec-5ef1-b51d-4158c314fad8', @expert_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 06:45:00', 1250, 680, 2100, 170,
        280, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003936000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'd0f7ff4b-5a6d-575e-aa41-e1d82a7f2831')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'd0f7ff4b-5a6d-575e-aa41-e1d82a7f2831', @customer_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 08:30:00', 920, 510, 1560, 130,
        180, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002968500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '2ae32ea1-7b5a-5b21-b455-03cc4b4bfdd4')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '2ae32ea1-7b5a-5b21-b455-03cc4b4bfdd4', @customer_06_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 11:45:00', 1100, 600, 1850, 150,
        240, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003475500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'e8807d46-a48e-5e47-aeb3-66106345d79f')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'e8807d46-a48e-5e47-aeb3-66106345d79f', @customer_id, NULL, 'FR-37', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 13:50:00', 980, 540, 1655, 135,
        200, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003131250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'a8a023f2-51ee-5580-9149-4dc137d99954')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'a8a023f2-51ee-5580-9149-4dc137d99954', @customer_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 03:15:00', 1150, 650, 1960, 160,
        250, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003731250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '7c36a663-1ed8-5ea3-a908-2d1b81503dbb')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '7c36a663-1ed8-5ea3-a908-2d1b81503dbb', @customer_06_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 05:40:00', 1320, 720, 2220, 180,
        300, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004162500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'ccac3a7c-ac8f-5dfa-99e9-9dd1574fac46')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'ccac3a7c-ac8f-5dfa-99e9-9dd1574fac46', @expert_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 07:50:00', 1480, 810, 2490, 200,
        350, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004661250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '028a343c-72f4-5d4c-a6fb-5b9d11106081')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '028a343c-72f4-5d4c-a6fb-5b9d11106081', @customer_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 09:30:00', 1200, 670, 2040, 170,
        270, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003867750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '594e069f-2372-55da-883f-010dcc2a2738')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '594e069f-2372-55da-883f-010dcc2a2738', @customer_06_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 12:15:00', 1390, 760, 2340, 190,
        320, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004389000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '70127fae-6616-5a8d-bc16-5b66a7410ac8')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '70127fae-6616-5a8d-bc16-5b66a7410ac8', @customer_id, NULL, 'FR-36', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 14:10:00', 1260, 700, 2135, 175,
        290, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004030500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '21b16c64-2b8c-5179-81c8-dd4375683727')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '21b16c64-2b8c-5179-81c8-dd4375683727', @customer_06_id, NULL, 'FR-47', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 04:00:00', 720, 410, 1230, 100,
        110, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002378250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '77f72c0a-7d33-5b6d-ac39-1c7a929b4598')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '77f72c0a-7d33-5b6d-ac39-1c7a929b4598', @expert_id, NULL, 'FR-47', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 07:00:00', 890, 490, 1505, 125,
        160, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002865750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '94df0e96-5e0b-5606-bf8e-a291a5ff1f02')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '94df0e96-5e0b-5606-bf8e-a291a5ff1f02', @customer_06_id, NULL, 'FR-47', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 08:45:00', 760, 430, 1300, 110,
        130, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002507250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '1e26d445-2c52-5e5d-841f-4a8d4f69faae')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '1e26d445-2c52-5e5d-841f-4a8d4f69faae', @expert_id, NULL, 'FR-47', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 10:50:00', 950, 530, 1615, 135,
        190, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.003078000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'dbe3a161-9891-5acc-bb4f-8e662ceca373')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'dbe3a161-9891-5acc-bb4f-8e662ceca373', @customer_06_id, NULL, 'FR-47', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 13:20:00', 820, 460, 1395, 115,
        150, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.002670000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '574049a6-e6ce-580e-93cd-142f5d4bcd97')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '574049a6-e6ce-580e-93cd-142f5d4bcd97', @customer_id, NULL, 'FR-34', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 03:45:00', 490, 270, 825, 65,
        60, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001583250000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'e08c63e8-41c4-5f40-8eeb-8969f28d05a9')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'e08c63e8-41c4-5f40-8eeb-8969f28d05a9', @customer_04_id, NULL, 'FR-34', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 06:15:00', 540, 290, 900, 70,
        80, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001701000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = 'f8258d91-1c17-5a25-af86-190d008c5ded')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        'f8258d91-1c17-5a25-af86-190d008c5ded', @customer_id, NULL, 'FR-34', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 09:15:00', 510, 280, 860, 70,
        70, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001647750000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '21144d65-2ee9-50de-8e60-299a6ca61a0c')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '21144d65-2ee9-50de-8e60-299a6ca61a0c', @customer_04_id, NULL, 'FR-34', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 11:05:00', 560, 310, 945, 75,
        90, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.001803000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '587e53de-f0e1-52c6-ab70-df27bf4e528f')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '587e53de-f0e1-52c6-ab70-df27bf4e528f', @expert_id, NULL, 'FR-21', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 05:30:00', 1350, 820, 2380, 210,
        280, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004686000000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '76403755-6d60-5b95-bbcc-79ca473875b4')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '76403755-6d60-5b95-bbcc-79ca473875b4', @expert_id, NULL, 'FR-21', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 08:20:00', 1520, 910, 2660, 230,
        340, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.005185500000
    );
END;
IF NOT EXISTS (SELECT 1 FROM AI_USAGE_TELEMETRY WHERE event_id = '843f1606-ea3b-5e19-89ee-46aa9de03e73')
BEGIN
    INSERT INTO AI_USAGE_TELEMETRY (
        event_id, user_id, anonymous_session_id, feature, model, service_tier,
        occurred_at, prompt_tokens, response_tokens, total_tokens, thinking_tokens,
        cached_tokens, tool_use_prompt_tokens, usage_status, pricing_version, estimated_cost_usd
    ) VALUES (
        '843f1606-ea3b-5e19-89ee-46aa9de03e73', @expert_id, NULL, 'FR-21', 'gemini-3.8-flash', 'STANDARD',
        '2026-10-29 12:30:00', 1410, 860, 2490, 220,
        310, 0, 'COMPLETE', 'gemini-standard-2026-10-23', 0.004898250000
    );
END;

COMMIT TRANSACTION;
