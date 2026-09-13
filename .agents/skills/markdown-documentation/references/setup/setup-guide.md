# Setup Guide Reference

Use this reference for local-environment setup documentation.

## 1. Goal

A new team member should be able to clone, configure, initialize, run, and verify the project using the documented steps without relying on undocumented tribal knowledge.

## 2. Typical Content

Include only confirmed project information:

- prerequisites and confirmed tool/runtime versions;
- repository clone steps;
- environment/configuration setup;
- safe explanation of required environment variables;
- database initialization/migrations/seeding when applicable;
- backend startup;
- frontend startup;
- verification step showing the setup works;
- known setup issues that the team has actually encountered and confirmed.

## 3. Do Not Invent or Leak

- Do not guess dependency versions.
- Do not write real passwords, tokens, secrets, or private keys.
- Do not invent troubleshooting steps for failures the project has not observed unless clearly labeled as generic guidance and the task requests it.

## 4. Responsibility Boundary

- Setup Guide -> getting the project running locally.
- Development Guide -> day-to-day technical development workflow after setup.
- Contributing Guide -> collaboration, branch/PR/review rules.
- README -> short entry point and quick links, not a duplicate of the full setup guide.

## 5. Verification

Prefer a concrete final check such as opening the application, calling a health endpoint, or running a confirmed smoke command when the repository provides one.
