# Minimal AGENTS.md Example

Use this reference only when the repository actually needs an `AGENTS.md` and does not already have an applicable convention.

Keep the real file concise. It should route agents to authoritative sources rather than duplicate full project documentation.

```markdown
# Agent Instructions

## Authoritative Sources

- Requirements: `docs/requirements/SRS.md`
- Product direction: `docs/requirements/PRD.md`
- Project overview: `README.md`
- Contribution workflow: `CONTRIBUTING.md`
- Changelog: `CHANGELOG.md`
- Repository contract: `.agents/repo-contract.yml` (if adopted)

## Project Layout

- Application source: `App/`
- Documentation: `docs/`
- GitHub configuration: `.github/`
- Agent assets: `.agents/`

## Documentation Rules

- Preserve accepted requirement IDs.
- Report equal-authority contradictions instead of silently choosing.
- Do not treat archived baselines as current sources of truth.
- Use repository-relative links when appropriate.
- Follow the project's documentation lifecycle and metadata policy.

## Collaboration Rules

- Follow `CONTRIBUTING.md` for branch, review, commit, and merge conventions.
```

This is an example, not a universal required structure. Adapt it to repository governance and do not invent paths that the repository does not use.
