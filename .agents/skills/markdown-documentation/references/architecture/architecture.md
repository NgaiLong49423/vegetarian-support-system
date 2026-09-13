# Architecture Documentation Reference

Use this reference for high-level software architecture documentation.

## 1. Responsibility

Architecture explains HOW THE SYSTEM IS STRUCTURED at a level useful to team members without requiring them to inspect source code first.
It must align with accepted requirements and decisions.

## 2. Default Level of Detail

Prefer high-level content:

- system context and external actors/systems;
- major applications/services/components/containers;
- responsibilities and boundaries;
- data stores;
- important integrations;
- communication paths/protocols when useful;
- deployment overview when useful;
- important constraints;
- basic trust/security boundaries when relevant;
- links to significant Decision Records.

## 3. Go Deeper Only When Needed

Module/package-level detail is appropriate when a subsystem is complex enough that responsibilities or dependencies would otherwise be unclear.

Do not turn architecture documentation into a catalog of controllers, DTOs, repositories, classes, methods, or package paths that change frequently.

## 4. Technology Stack Is Not Architecture

`React + Spring Boot + PostgreSQL` identifies technologies.
Architecture must also explain which major parts exist, what they own, and how they interact.

## 5. Security Minimum

For typical web projects, show or explain important boundaries when relevant, such as:

- client vs backend trust boundary;
- authentication/authorization responsibility;
- secrets remaining server-side;
- external integrations.

Do not create an enterprise threat-modeling framework unless the project adopts one.

## 6. Diagrams

Use diagrams only when they clarify structure or interactions.
C4-style context/container views are often sufficient for small projects but are not mandatory.
Critical diagram information should have enough textual explanation to remain understandable.

## 7. Review Questions

- Can a new team member identify major parts and responsibilities?
- Are system boundaries and integrations clear?
- Is the abstraction level stable enough to survive normal code refactoring?
- Does architecture conflict with accepted SRS/Decision Records?
- Are significant choices linked rather than re-explained inconsistently?
