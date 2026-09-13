> **Document:** Workflow Evidence Register  
> **File:** `docs/decisions/WORKFLOW-SOURCES.md`  
> **Version:** v1.4.0  
> **Created:** 2026-09-08  
> **Last Updated:** 2026-09-12  
> **Status:** Active  

# Workflow Sources and Evidence

## Purpose

This document records which parts of the team workflow come from external guidance or platform behavior and which parts are conventions chosen by this team. External sources inform decisions; they do not automatically override the team's documented constraints.

## Evidence Classes

| Class | Meaning |
|---|---|
| Platform behavior | Behavior documented by the tool vendor that the workflow must account for. |
| Framework guidance | Guidance defined by a recognized framework. It may require local adaptation. |
| Complementary practice | A useful practice that is not a mandatory part of the framework. |
| Team convention | A local decision chosen for this student team and project template. |

## Source Register

| Source | Evidence class | What it establishes | How this template uses it |
|---|---|---|---|
| [The Scrum Guide (2020)](https://scrumguides.org/scrum-guide.html) | Framework guidance | Product Backlog refinement adds detail, order, and size; the people doing the work are responsible for sizing; work must satisfy the Definition of Done before it is treated as complete. | Supports source-backed refinement, developer participation in estimates, testable completion criteria, and a clear Definition of Done. |
| [Scrum.org: Ready or Not? Demystifying the Definition of Ready in Scrum](https://www.scrum.org/resources/blog/ready-or-not-demystifying-definition-ready-scrum) | Complementary practice | A Definition of Ready can help a team clarify work before starting, but it is not a formal Scrum artifact and should not become a heavy gate or substitute for collaboration. | The team uses a short readiness checklist before moving an Issue from `Planning` to `In Progress`, then reviews and adapts it. |
| [GitHub Docs: About Projects](https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/about-projects) | Platform behavior | GitHub Projects provides configurable views, fields, and automation without imposing one project method. | Supports the team's custom five-state board and fields. The exact states remain a team convention. |
| [GitHub Docs: Using the built-in automations](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-built-in-automations) | Platform behavior | Built-in workflows can update item status when Pull Requests merge or Issues close. | The generic merged-PR-to-Done automation stays disabled unless it can distinguish a release merge to `main` from a feature merge to `develop`. |
| [GitHub Docs: Linking a pull request to an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue) | Platform behavior | Closing keywords close linked Issues when the Pull Request is merged into the repository's default branch. | With `main` as the default branch, feature PRs to `develop` do not represent Done; the release PR to `main` must reference the included Issues. |
| [GitHub Docs: About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches) | Platform behavior | Branch protection can require reviews and status checks before merging. | Supports protecting `main` and `develop` once repository roles and required checks are confirmed. The exact protection settings are still undecided. |
| [Google Engineering Practices: Code Review](https://google.github.io/eng-practices/review/) | Complementary practice | Reviewers should be qualified for the relevant change and responsive; different reviewers may cover different parts. | One reviewer is sufficient by default, while database, authentication, shared-structure, and release changes require broader review. |
| [A successful Git branching model](https://nvie.com/posts/a-successful-git-branching-model/) | Complementary practice | Describes a long-lived integration branch and a stable production branch. The author also notes that this model is not universal. | The template adapts `develop` as integration and `main` as the stable demo branch because the team expects weekly demo releases. |

## Decision Classification

| Workflow decision | Classification | Notes |
|---|---|---|
| `Backlog -> Planning -> In Progress -> Review -> Done` | Team convention | GitHub Projects permits this configuration but does not prescribe these five states. |
| `feature branch -> develop -> main` | Adapted team convention | Chosen for integration and weekly demos; it should be reconsidered if the project moves to continuous delivery. |
| Keep an Issue in `Review` after merge to `develop` | Team convention informed by platform behavior | Prevents an integration merge from being confused with release completion. |
| `Done` only after release merge to `main` and Issue closure | Team Definition of Done | Adapts the Scrum completion concept to the repository's release model. |
| Definition of Ready before `In Progress` | Complementary team practice | A lightweight collaboration checklist, not an official Scrum artifact or a demand for a perfect specification. |
| Task duration of 4–5 calendar days | Team convention | Chosen for a student schedule; not derived from Scrum or GitHub. |
| `Target Date` ends at technical completion and merge into `develop` | Team convention | Separates an owner's delivery deadline from the shared release schedule and prevents false overdue reminders while an integrated Issue waits in `Review`. |
| Story Point scale `1, 2, 3, 5, 8`, with `8` requiring decomposition | Team convention informed by relative estimation practice | Used for planning and load balancing, never for individual performance scoring. |
| One default reviewer and two for selected high-risk changes | Team convention informed by review guidance | Repository rules should be configured only after roles and permissions are confirmed. |
| One owner, one reviewer/backup and one default `In Progress` Issue per member | Team convention | Chosen for a five-person student team to reduce hidden work and overloaded owners; not prescribed by Scrum or GitHub. |
| Decisions that change requirements, API, schema, architecture, core dependencies or workflow need at least 3/5 agreement | Team convention | Creates a recorded majority rule while keeping course requirements and security constraints non-negotiable. |

| Technical completion gate before merging to `develop` | Team convention informed by Scrum quality guidance and GitHub merge controls | Requires traceability, verification evidence, review approval, resolved blocking feedback, and applicable documentation or database updates. |

| Release gate before merging `develop` to `main` | Team convention informed by the Scrum Definition of Done and GitHub branch controls | Requires an integrated, usable release candidate, verification on the latest commit, two approvals, no bypass, traceable Issue closure, and a post-merge smoke check. |

## Review Policy

- Revisit a convention when the team size, delivery cadence, course requirements, repository permissions, or deployment model changes.
- Prefer primary and vendor documentation when platform behavior is in question.
- Record a new source or changed interpretation here before changing a settled workflow rule.
- Treat blog posts and examples as guidance, not as mandatory standards.
