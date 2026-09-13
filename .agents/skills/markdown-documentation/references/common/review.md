# Documentation Review Reference

Use this reference for audits, reviews, and final validation.

## Review Checklist

### Scope and Authority

- [ ] The requested scope is covered.
- [ ] Applicable repository governance is followed.
- [ ] The authoritative source for each affected concern is identified.
- [ ] Out-of-scope issues are reported rather than silently fixed.

### Accuracy

- [ ] No unsupported requirement, rule, actor, technology, endpoint, entity, target, or decision was invented.
- [ ] Unknown information remains explicit.
- [ ] Historical artifacts are not mistaken for current sources of truth.

### Change Safety

- [ ] Accepted/baselined content was not semantically changed without authorization.
- [ ] Editorial changes preserve meaning.
- [ ] Directly affected artifacts remain consistent after authorized semantic changes.

### Requirements and Traceability

- [ ] IDs remain stable.
- [ ] Requirement readiness issues are identified when relevant.
- [ ] Existing UC/Feature/Test traceability is preserved when those artifacts exist.
- [ ] No fake traceability artifact was created merely to make the documentation look complete.

### Structure and Readability

- [ ] Heading hierarchy is valid.
- [ ] Terminology is consistent.
- [ ] Tables/lists are appropriate.
- [ ] Basic accessible-Markdown rules are respected where applicable.

### Links and References

- [ ] Internal links affected by the task resolve correctly when repository access permits verification.
- [ ] Renamed/moved paths no longer leave known stale references.
- [ ] References point to the correct source of truth.

### Open Items

- [ ] Blocking open items are distinguished from non-blocking ones.
- [ ] Stale TBDs are reported.

## Completion Criteria

A documentation task is complete when the requested change is present, directly affected content remains coherent,
no obvious contradiction was introduced, unknowns are explicit, and another team member can understand the result without hidden assumptions.
