# Research Note Reference

Use this reference for exploratory technical research that may inform later project decisions.

## 1. Research Is Not a Decision

A research note answers “what did we learn?”
A Decision Record answers “what did the project decide and why?”

Do not update SRS/Architecture as if a recommendation were accepted merely because a research note favors it.

## 2. Recommended Content

Include only useful sections such as:

- Question/problem
- Context
- Sources/evidence
- Findings/options
- Experiment setup and observations when applicable
- Limitations/unknowns
- Applicability to this project
- Recommendation, explicitly labeled
- Open questions
- Decision status / linked accepted decision when one later exists

## 3. Source Quality

Prefer stronger evidence when important claims matter:

1. Official standard/specification/documentation.
2. Primary vendor/maintainer source.
3. Academic paper or reputable technical publication.
4. Established engineering documentation/blog.
5. Community discussion such as GitHub issues, Stack Overflow, Reddit.
6. AI-generated content.

Lower-ranked sources are not forbidden; they are often useful for practical experience, bugs, trade-offs, and workarounds.
Important claims from weaker sources should be checked against stronger evidence when possible.

## 4. Separate Fact, Interpretation, Recommendation, Decision

Label the distinction when confusion is possible:

- Fact -> directly supported by evidence.
- Interpretation -> how the evidence is understood in context.
- Recommendation -> proposed action based on evidence.
- Decision -> accepted project choice; it requires project authority and normally belongs in the appropriate authoritative artifact.

## 5. Conflicting Sources

When sources disagree:

1. Do not silently pick one.
2. Compare authority and recency.
3. Check whether assumptions/contexts differ.
4. Distinguish factual disagreement from different recommendations.
5. Record unresolved conflict explicitly.
6. Escalate only when unresolved disagreement affects a project decision.
