## ADDED Requirements

### Requirement: Detail view MUST not silently display blank quote fields
The system MUST avoid a state where a quote object exists but core quote fields are blank, resulting in the detail view only showing placeholders without any error or fallback.

#### Scenario: Quote object is incomplete
- **WHEN** the quote provider returns a quote for the selected symbol but `lastPrice` is missing
- **THEN** the system treats the quote as unavailable and does not use it as the primary display quote

### Requirement: Detail view SHALL provide an intraday-derived fallback quote
When a real-time quote is unavailable, the system SHALL derive a minimal fallback quote from intraday history points to keep the detail view informative.

#### Scenario: Intraday history exists but quote is unavailable
- **WHEN** the user opens the detail view and intraday history points are available but real-time quote is unavailable
- **THEN** the system derives and displays `lastPrice`, `open`, `high`, and `low` from intraday points

### Requirement: Detail view SHALL surface a user-visible status message when using fallback
The system SHALL show a status message when the detail view is using a derived or fallback quote instead of a real-time provider quote.

#### Scenario: Showing derived quote message
- **WHEN** the detail view displays an intraday-derived quote
- **THEN** the system shows a message indicating the quote is derived from intraday history

