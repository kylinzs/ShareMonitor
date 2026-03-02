## ADDED Requirements

### Requirement: Project provides a code style guideline document
The project SHALL provide a code style guideline document that is easy to discover and that defines team conventions used in this repository.

#### Scenario: Developers can find the guideline
- **WHEN** a developer joins the project and searches for “code style” or “规范”
- **THEN** the repository contains a clear guideline document (e.g., `CODE_STYLE.md` or `docs/code-style.md`) describing conventions

### Requirement: New code files include Chinese documentation comments
The project SHALL require that newly added code files include Chinese documentation comments to explain file purpose and public APIs.

#### Scenario: New Kotlin file meets documentation minimum
- **WHEN** a developer adds a new Kotlin source file
- **THEN** the file includes Chinese documentation (at least file-level purpose and KDoc for public types/functions)

#### Scenario: Review checklist enforces the rule
- **WHEN** a code review is performed for a change that introduces new Kotlin files
- **THEN** the reviewer checks the guideline rule and requests changes if Chinese documentation comments are missing

