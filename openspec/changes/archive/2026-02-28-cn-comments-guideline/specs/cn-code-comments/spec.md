## ADDED Requirements

### Requirement: Core Kotlin types have Chinese KDoc
The codebase SHALL provide Chinese KDoc for core Kotlin types so that readers can understand intent, usage boundaries, and key assumptions without reverse-engineering implementation details.

#### Scenario: Domain models are documented
- **WHEN** a developer opens a domain model file under `app/src/main/java/**/domain/model/`
- **THEN** the file’s main types and key fields have Chinese KDoc describing purpose and field meaning

#### Scenario: Data layer is documented
- **WHEN** a developer opens repository/data-source code under `app/src/main/java/**/data/`
- **THEN** repositories and data sources include Chinese KDoc explaining responsibilities, caching/refresh behavior, and notable constraints

#### Scenario: ViewModels are documented
- **WHEN** a developer opens ViewModel code under `app/src/main/java/**/ui/viewmodel/`
- **THEN** each ViewModel includes Chinese KDoc describing UI state, main actions, and non-obvious logic (e.g., throttling, fallbacks)

### Requirement: Comments avoid noise and stay aligned with behavior
The codebase SHALL avoid redundant “translation” comments and SHALL keep comments focused on stable intent and contracts to reduce drift.

#### Scenario: Obvious UI layout is not over-commented
- **WHEN** a developer reviews UI Composables
- **THEN** comments are not added for self-evident layout code, and only non-obvious behavior/assumptions are documented

