## ADDED Requirements

### Requirement: Repository MUST exclude local-only configuration and build artifacts
The repository MUST avoid committing local-only configuration and generated build artifacts, including but not limited to `local.properties`, `**/build`, and packaged outputs (e.g. `*.apk`).

#### Scenario: Staging changes for first commit
- **WHEN** the user stages files for the initial commit
- **THEN** local-only configuration and build artifacts are not included in the staged set

### Requirement: Repository SHALL provide a reproducible GitHub publish workflow
The system SHALL define a reproducible workflow to publish the project to GitHub, including initializing git, creating an initial commit, configuring the default branch as `main`, and pushing to a remote `origin`.

#### Scenario: Publishing to GitHub over HTTPS
- **WHEN** the user creates a GitHub repository and sets `origin` using an HTTPS URL
- **THEN** the user can push the `main` branch to GitHub successfully

#### Scenario: Publishing to GitHub over SSH
- **WHEN** the user creates a GitHub repository and sets `origin` using an SSH URL
- **THEN** the user can push the `main` branch to GitHub successfully

