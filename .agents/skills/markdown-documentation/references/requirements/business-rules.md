# Business Rules Reference

Use this reference for domain rules, policies, eligibility conditions, and constraints that govern system behavior.

## 1. BR vs FR

A BR states a domain/policy rule the system must respect.
An FR states behavior the system provides.

Example:

```text
FR: A member can generate a weekly meal plan.
BR: A weekly meal plan contains seven calendar days.
```

## 2. Define Once, Reference Many

When the same rule constrains multiple requirements, define it once under a stable BR identifier and reference that ID.
Do not copy the same rule into many FRs with slightly different wording.

Example:

```text
BR-05 — Allergen Exclusion
Meals containing a user's declared allergen must not be recommended.

FR-12 — Recommend Meals
Related rules: BR-05

FR-18 — Generate Weekly Meal Plan
Related rules: BR-05
```

## 3. Keep BRs Testable

Prefer objectively checkable rules.
If a condition/threshold is unconfirmed, keep it explicit as TBD rather than inventing a value.

## 4. BR Lifecycle

Preserve accepted BR IDs.
Use lifecycle/change handling from `references/common/document-lifecycle.md` for BRs that are deferred, out of scope, retired, or removed from an active baseline.

Do not retire a BR automatically merely because one related FR is retired. Determine whether the BR still constrains other active behavior.

## 5. Splitting BRs into a Separate File

A BR section may remain inside SRS when manageable.
If BR content becomes large, independently maintained, or widely referenced, propose a separate `BUSINESS-RULES.md` (or repository convention) before creating it.

## 6. Review Questions

- Is this a real domain/policy rule rather than implementation detail?
- Is it supported by confirmed project information?
- Can it be tested objectively?
- Is it duplicated elsewhere?
- Does it still apply to active requirements?
- Are referenced FRs real?
