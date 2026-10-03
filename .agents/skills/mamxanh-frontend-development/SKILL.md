---
name: mamxanh-frontend-development
description: >
  Implement, review, or validate Frontend changes for the Mâm Xanh React application.
  Use repository-first discovery, preserve the existing visual identity and component
  conventions, and apply responsive, accessible, production-quality UI guidance without
  importing a competing design workflow. Use for .tsx, .ts, .css, component, page,
  route, form, layout, responsive, UI, UX, and browser-visible changes.
metadata:
  category: project-local-domain
  project: mamxanh
  source: adapted-from-harness-skills
  version: "v1.0.0"
---

# Mâm Xanh Frontend Development

This is the project-local Frontend domain skill. It supplies Frontend expertise to the
existing `.agents/workflows/implement-fr-issue.md` workflow; it does not replace that
workflow and does not create a `hs-plan -> hs-build -> hs-test -> hs-ship` pipeline.

## Authority and hard gate

- Follow the current task, `AGENTS.md`, `CONTRIBUTING.md`, the applicable nested `AGENTS.md`,
  and the authoritative SRS/API/architecture documents before this skill or generic upstream
  guidance.
- Do not change application code until the governing workflow has confirmed the Issue/FR
  scope and Acceptance Criteria, or the user has explicitly requested implementation.
- Do not edit `docs/diagrams/` or human-controlled design artifacts unless the current task
  explicitly names the artifact and authorizes the edit.
- Do not redesign the application, replace core technologies, introduce unapproved libraries,
  or invent a new design direction for a bounded feature. Auxiliary or already-confirmed
  dependencies may be added when the Issue genuinely requires them and repository Engineering
  Autonomy rules permit the change.

## Confirmed project baseline

Verify source files and manifests before relying on this list. The current application uses
React, TypeScript, Vite, Tailwind CSS, and React Router, with npm and Playwright.

- Frontend source under `app/mamxanh-frontend/`, with shared components in `src/components/`,
  pages in `src/pages/`, route composition in `src/App.tsx`, global styling in
  `src/index.css`, and existing Figma Make imports/assets kept as project context.
- The real authority remains the current `package.json`, source code, README, nested
  `AGENTS.md`, and applicable architecture/API documents. Exact versions are owned by
  `package.json` and the confirmed technology baseline. Never select or upgrade a version
  from this skill text.

## Required context order

Before editing, read only the relevant slice in this order:

1. `AGENTS.md`, `CONTRIBUTING.md`, and the applicable `app/mamxanh-frontend/AGENTS.md`.
   Read `.agents/repo-contract.yml` only when the task needs machine-readable policy detail
   not already available in `AGENTS.md`.
2. `app/mamxanh-frontend/README.md` and `package.json`.
3. The relevant SRS/Acceptance Criteria and generated Backend OpenAPI (`/v3/api-docs`, or the
   artifact/spec generated from the same branch commit) before implementing/reviewing an API-backed
   feature. During migration, use `docs/api/openapi.yaml` only as planned contract for endpoints
   not present in the runtime spec; do not assume those endpoints are implemented.
4. Existing route composition, the target page, at least one nearby component, and
   `src/index.css` or the existing token/style source.
5. Existing Figma/Figma Make/Stitch material only when it is present and relevant. Treat it
   as reference, not permission to overwrite human-controlled artifacts.

Do not require `PRODUCT.md` or `DESIGN.md`. Mâm Xanh's existing code, README, CSS, assets,
Figma context, SRS, and accepted decisions are the project context. If context is missing,
record the gap and use the smallest evidence-backed inference needed for the Issue.

## Implementation principles

Preserve the existing design identity before adding novelty:

- Reuse existing components, tokens, typography, color, spacing, icon treatment, routing,
  loading/error/empty states, and interaction patterns before creating alternatives.
- Keep design tokens semantic and centralized. Extract a repeated pattern only when it has
  the same intent and is genuinely reused; do not create a generic component for one use.
- Treat responsive behavior as structural adaptation, not merely smaller desktop pixels.
  Check narrow mobile, tablet, and desktop behavior for changed visible surfaces.
- Use readable hierarchy, sensible line lengths, stable spacing, and accessible contrast.
  Every interactive control needs a visible keyboard focus path, an accessible name, and
  an appropriate disabled/loading/error state where applicable.
- Use motion only to communicate state or feedback; respect reduced-motion preferences and
  avoid decorative choreography.
- Write UX copy that names the action, explains validation/errors, and teaches empty states.
- Avoid generic AI UI patterns: gratuitous gradients, glassmorphism, card grids, decorative
  badges, excessive rounded containers, random typography, and invented visual systems.

For API-backed UI, keep server-side validation and authorization authoritative. Model loading,
success, empty, validation failure, authorization failure, network failure, and retry behavior
explicitly rather than hiding errors or assuming mock data is production behavior.

- Before adding an API call, inspect and reuse the repository's existing API client, service
  layer, base URL, authentication, error handling, and response conventions. Do not introduce
  ad-hoc `fetch`/`axios` calls or hard-coded Backend URLs inside pages or components when an
  established integration boundary exists.
- If an API-backed Issue requires integration and no reusable client/service boundary exists
  yet, inspect the confirmed Technology Stack and establish the smallest reusable integration
  boundary before wiring pages or components. Do not scatter HTTP calls, base URLs, token
  attachment, or response parsing across UI components.

## Capability routing

When the workflow identifies a changed scope, state the route before implementation:

`Routing: frontend (<changed files or surface>) -> mamxanh-frontend-development`

Use this skill for `*.tsx`, `*.ts`, `*.css`, frontend config, `src/components/`, `src/pages/`,
route/UI code, and browser-visible behavior. A full-stack change loads this skill together
with `mamxanh-backend-development`; neither skill owns the other domain's decisions.

## Validation and review

Choose checks proportional to the changed surface and report actual output:

- TypeScript: `npm run lint` from `app/mamxanh-frontend/`.
- Production bundle: `npm run build` from `app/mamxanh-frontend/`.
- Browser behavior: the smallest relevant Playwright command using the repository's
  existing config; distinguish exploratory UI/mock evidence from real Backend/database E2E.
- Review changed screens for responsive layout, focus/keyboard behavior, contrast, semantics,
  loading/error/empty states, copy, and consistency with nearby components.

Do not claim browser, Backend, or database behavior from TypeScript/build output alone. Inspect
the final diff for unrelated files, secrets, `.env` values, generated output, and accidental
design-direction changes.

## Provenance

Adapted from `Unibean9/harness-skills`:

- Upstream path: skills/hs-frontend-development/SKILL.md.
- Upstream references consulted: the `product`, `tokens`, `responsive`, `color`, `type`,
  `space`, `interaction-design`, `check`, and `review` reference documents under the
  upstream Frontend skill.
- Upstream shared references consulted: `domain-routing`, `evidence-policy`, and `hard-gate`.

The upstream Frontend flow itself credits `ThinhTP204/fk-skills`; this project intentionally
does not copy that repository's files. This skill is a project-specific adaptation and
intentionally omits upstream installer, hooks, subagents, workflow skills, `PRODUCT.md`/
`DESIGN.md` setup, live-mode tooling, and deterministic detector. Preserve applicable upstream
attribution when materially copying or adapting content.
