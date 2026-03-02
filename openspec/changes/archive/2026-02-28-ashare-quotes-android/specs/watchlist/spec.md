## ADDED Requirements

### Requirement: Watchlist persists locally
The system SHALL persist the watchlist on-device and restore it on app restart.

#### Scenario: Restart restores watchlist
- **WHEN** the user restarts the app after adding symbols to the watchlist
- **THEN** the watchlist displays the same symbols in the same order as before

### Requirement: Watchlist enforces uniqueness
The system MUST treat `exchange + code` as the unique key for a watchlist item and MUST prevent duplicates.

#### Scenario: Adding an existing symbol does not create duplicates
- **WHEN** the user attempts to add a symbol already present in the watchlist
- **THEN** the watchlist remains unchanged and shows a non-blocking confirmation

### Requirement: User can remove symbols from watchlist
The system SHALL allow the user to remove a symbol from the watchlist.

#### Scenario: Remove succeeds
- **WHEN** the user removes a symbol from the watchlist
- **THEN** the symbol no longer appears in the watchlist

### Requirement: User can reorder and pin watchlist items
The system SHALL allow the user to reorder items, and SHALL allow pinning items that always appear at the top.

#### Scenario: Reorder changes display order
- **WHEN** the user reorders watchlist items
- **THEN** the watchlist displays in the new order and persists it

#### Scenario: Pin keeps item at top
- **WHEN** the user pins an item and then performs a refresh
- **THEN** the pinned item remains above unpinned items

### Requirement: Watchlist shows quote freshness
The system SHALL show a "last updated" timestamp for watchlist quotes, and MUST indicate when displayed data is stale.

#### Scenario: Data becomes stale
- **WHEN** the latest successful quote update is older than the configured staleness threshold
- **THEN** the watchlist marks the quote data as stale and keeps displaying the last known values

