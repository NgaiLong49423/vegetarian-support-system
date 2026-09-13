---
name: changelog-automatic
description: Maintain the project's dated English changelog using CONTRIBUTING.md; verify dates, topic scope and commit/PR evidence before recording changes.
risk: medium
source: customized
version: v2.0.0
created_date: 2026-06-29
last_updated_date: 2026-09-13
---

# Changelog Maintenance

## Trigger

Use when updating CHANGELOG.md, converting historical entries, or preparing changelog content for a requested release. Do not load for unrelated source changes unless a notable changelog update is required.

## Authority

Read [CONTRIBUTING.md — Changelog Format](../../../CONTRIBUTING.md#changelog-format) before writing. That section defines the format, language, date ordering, categories and status lifecycle. Do not substitute generic Keep a Changelog / Unreleased examples or maintain a second format here. Follow AGENTS.md for agent-specific read scope and authorization.

## Evidence procedure

1. Read the affected changelog entries and identify the coherent topic.
2. Inspect relevant working-tree differences, file-specific Git history and user-confirmed changes. Avoid scanning unrelated history.
3. Distinguish uncommitted work from a verified commit checkpoint. A draft entry is not proof of implementation, merge or deployment.
4. Use an existing verified PR association when available. Inspect a referenced PR through read-only tools if necessary; omit unknown PR references and never fabricate them or reuse a reference project's PRs.
5. Convert existing history without changing its dates or dropping meaningful decisions. If the exact change date is unavailable, identify an evidenced checkpoint date explicitly. Do not treat example blocks as real history.
6. Write the entry in the format defined by CONTRIBUTING.md. Update an existing entry for the same work rather than duplicating it.
7. Update document metadata according to the repository metadata procedure; preserve Created and distinguish document versions from project releases.
8. Check date ordering, required sections, English prose, references and agreement with the evidence. Report results in the conversation.

## Limits

This procedure does not authorize commits, tags, PR creation, merges, releases or automation. Apply the user-requested scope and the contribution rules. Do not add release tooling for a manual changelog change.
