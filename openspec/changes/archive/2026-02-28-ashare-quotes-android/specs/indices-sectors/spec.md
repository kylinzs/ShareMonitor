## ADDED Requirements

### Requirement: App provides a Market entry
The system SHALL provide a visible entry to a "行情" screen from the main navigation.

#### Scenario: User opens Market
- **WHEN** the user taps the "行情" entry
- **THEN** the system navigates to the Market screen

### Requirement: Indices list displays key quote fields
The system SHALL display a list of major indices with key quote fields including `name`, `code`, `lastPrice`, `change`, `changePct`, and `quoteTime` when available.

#### Scenario: Indices list renders
- **WHEN** index quote data is available
- **THEN** the Indices list shows each index with the required fields and last updated time

### Requirement: Sectors list is available and sortable
The system SHALL display a list of sectors and SHALL support at least one sorting mode (e.g., by changePct).

#### Scenario: User views sectors sorted by changePct
- **WHEN** the user selects the sort mode "涨跌幅"
- **THEN** the system shows sectors ordered by `changePct`

### Requirement: Sector detail shows constituents
The system SHALL provide a sector detail view that displays the sector's constituent symbols with key quote fields.

#### Scenario: Open sector and view constituents
- **WHEN** the user selects a sector from the sectors list
- **THEN** the system shows a list of constituent symbols for that sector

### Requirement: Constituents can be added to watchlist
The system SHALL allow the user to add a constituent symbol to the watchlist from the sector detail view, and MUST prevent duplicates.

#### Scenario: Add constituent to watchlist
- **WHEN** the user taps "Add" on a constituent symbol not currently in the watchlist
- **THEN** the system adds it to the watchlist and updates UI state accordingly

#### Scenario: Add existing constituent does not duplicate
- **WHEN** the user taps "Add" on a constituent symbol already in the watchlist
- **THEN** the system does not create a duplicate and shows a non-blocking confirmation

### Requirement: Indices/Sectors refresh respects minimum interval
The system MUST enforce the configured minimum refresh interval (default 15 minutes) for indices, sectors, and constituent quote refreshes.

#### Scenario: Refresh within interval uses cached data
- **WHEN** the user triggers refresh within the configured refresh interval since the last successful update
- **THEN** the system does not perform a network request and continues to display cached data with a user-visible message

### Requirement: Indices/Sectors failures are user-visible
If indices/sectors data loading fails, the system MUST inform the user and provide a retry action.

#### Scenario: Load fails and user retries
- **WHEN** loading indices or sectors fails and the user taps retry
- **THEN** the system attempts to load again and updates the UI on success

### Requirement: Market screen uses Eastmoney in MVP
The system MUST use Eastmoney as the data source for the Market ("行情") screen in MVP, regardless of the selected quote provider.

#### Scenario: Quote provider switch does not affect market data source
- **WHEN** the user switches the quote provider to Tencent
- **THEN** the Market screen continues to load indices and sectors from Eastmoney
