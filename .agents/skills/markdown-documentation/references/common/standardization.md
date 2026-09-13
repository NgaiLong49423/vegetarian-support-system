# Markdown Standardization Reference

Use this reference when improving structure, terminology, readability, or Markdown style without changing project meaning.

## 1. Heading Hierarchy

Use headings to represent structure, not visual size.

```text
# Document Title
## Major Section
### Subsection
#### Detailed Section
```

Avoid unexplained heading-level jumps.

## 2. Writing Style

Prefer direct, precise, maintainable technical language.
Avoid unsupported marketing terms such as “smart”, “optimal”, “seamless”, or “advanced”.

## 3. Terminology

Use one established term for one concept.
If terminology conflicts are semantically meaningful, report the conflict instead of silently normalizing it.

## 4. Tables and Lists

Use tables for naturally tabular comparisons or mappings.
Use meaningful table headers.
Avoid long prose inside table cells when subsections would be clearer.

Use numbered lists for sequences and bullets when order does not matter.

## 5. Examples

Examples may clarify behavior but must not silently define new requirements or decisions.

## 6. Diagrams

Use diagrams only when they improve understanding and the repository supports the chosen format.
Do not make a diagram the only carrier of critical information; add a concise textual explanation when needed.

## 7. Accessible Markdown Basics

Keep accessibility lightweight but intentional:

- Informative images should have meaningful alt text.
- Link text should describe its destination or purpose; avoid bare “click here” or “here”.
- Heading order should reflect document structure.
- Data tables should have meaningful headers.
- Critical information shown in an image/diagram should also be available in text when needed for understanding.

These are authoring rules, not a claim of full WCAG compliance.

## 8. Standardization Boundary

Standardization may change formatting, grammar, and presentation, but must not change accepted project meaning.
If a proposed cleanup may alter meaning, handle it as a semantic change.
