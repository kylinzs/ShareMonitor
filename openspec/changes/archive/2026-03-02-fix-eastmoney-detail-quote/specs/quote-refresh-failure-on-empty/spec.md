## ADDED Requirements

### Requirement: Quote refresh MUST fail on empty results
The system MUST treat a successful network call that returns no quotes for requested symbols as a refresh failure, rather than a success.

#### Scenario: Data source returns empty quote list
- **WHEN** the system refreshes quotes for one or more symbols and the quote provider returns an empty list
- **THEN** the refresh outcome is marked as failed and a user-visible message is available

### Requirement: Quote refresh SHALL expose a user-friendly error message for empty results
When quote refresh fails due to empty results, the system SHALL provide a clear, user-friendly message that helps the user understand that no data was retrieved.

#### Scenario: Display empty-result message
- **WHEN** quote refresh fails due to empty results
- **THEN** the UI can display a message equivalent to “no quote data retrieved”

