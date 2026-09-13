# API Documentation Reference

Use this reference for Markdown API guides and API-contract documentation boundaries.

## 1. Source of Truth for API Contract

If the repository adopts OpenAPI/Swagger as the maintained contract, treat it as the source of truth for detailed endpoint definitions such as:

- paths and HTTP methods;
- request/response schemas;
- status codes;
- parameter definitions.

Do not duplicate the full contract into Markdown and create a second competing source of truth.

If no OpenAPI/Swagger contract exists, the repository may designate Markdown as the current API contract source. Do not invent endpoints or schemas.

## 2. What `API.md` Is Good For

A Markdown API guide may document:

- base URL/environment convention;
- authentication approach;
- common error format;
- pagination/filtering conventions;
- important request flows;
- integration notes;
- links to the detailed contract/source of truth.

## 3. Consistency

When a change affects a documented API behavior, update the owning contract first or according to repository workflow, then update explanatory Markdown as needed.

Do not describe endpoints that no longer exist in the active contract.

## 4. Security Basics

Do not expose real credentials, API keys, tokens, or private secrets in examples.
Use obvious placeholders when examples require credentials.

## 5. Review Questions

- Is the detailed contract source of truth clearly identified?
- Does Markdown duplicate details that will drift?
- Are authentication/error conventions clear?
- Are examples grounded in real project behavior?
- Are removed/retired endpoints absent from active documentation unless intentionally documented as historical/deprecated behavior?
