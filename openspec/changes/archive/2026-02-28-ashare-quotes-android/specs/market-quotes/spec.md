## ADDED Requirements

### Requirement: System can fetch normalized quotes for multiple symbols
The system SHALL fetch quote data for a set of symbols and normalize it into a common model.

#### Scenario: Batch quote fetch succeeds
- **WHEN** the watchlist requests quotes for N symbols
- **THEN** the system returns a quote model for each symbol with `lastPrice`, `change`, `changePct`, and `quoteTime`

### Requirement: System supports quote refresh
The system SHALL provide a user-triggered refresh that requests the latest quotes and updates the UI state.

#### Scenario: Pull-to-refresh updates quotes
- **WHEN** the user triggers refresh on the watchlist
- **THEN** the system fetches the latest quotes and the list updates with new values

### Requirement: Quote refresh respects minimum interval
The system MUST enforce a minimum quote refresh interval, and MUST NOT perform network quote refreshes more frequently than the configured interval.

#### Scenario: Refresh within interval uses cached data
- **WHEN** the user triggers refresh within the configured refresh interval since the last successful update
- **THEN** the system does not perform a network request and continues to display the last cached quotes with a user-visible message

#### Scenario: Refresh after interval performs network request
- **WHEN** the user triggers refresh after the configured refresh interval has elapsed
- **THEN** the system performs a network request and updates quotes on success

### Requirement: History refresh respects minimum interval
The system MUST enforce the same minimum refresh interval for history data as for quote data.

#### Scenario: History refresh within interval uses cached data
- **WHEN** the detail screen requests history refresh within the configured refresh interval since the last successful history update
- **THEN** the system does not perform a network request and continues to display cached history with a user-visible message

### Requirement: System handles quote fetch failures gracefully
If quote fetching fails, the system MUST keep displaying last known cached data (if any) and MUST show an error state.

#### Scenario: Quote fetch fails with cached data available
- **WHEN** the quote fetch fails and cached quotes exist
- **THEN** the system displays cached quotes, marks them as stale, and shows a non-blocking error message

#### Scenario: Quote fetch fails with no cached data
- **WHEN** the quote fetch fails and no cached quotes exist
- **THEN** the system shows an error state with a retry action

### Requirement: Quote requests are rate-limited client-side
The system MUST rate-limit quote requests to avoid exceeding provider limits.

#### Scenario: Rapid refresh is throttled
- **WHEN** the user triggers refresh repeatedly within the throttling window
- **THEN** the system performs at most one network request within that window

### Requirement: Data source is pluggable
The system SHALL obtain quotes through a data source abstraction and SHALL support switching between multiple real provider sources.

#### Scenario: Switching data source changes quote origin
- **WHEN** the user switches the quote data source in settings
- **THEN** subsequent quote fetches use the selected source
