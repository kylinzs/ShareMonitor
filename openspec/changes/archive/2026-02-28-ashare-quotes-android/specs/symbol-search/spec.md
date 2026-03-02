## ADDED Requirements

### Requirement: User can search A-share symbols
The system SHALL allow the user to search A-share symbols by stock code prefix and by name substring.

#### Scenario: Search returns matching symbols
- **WHEN** the user enters a query and triggers search
- **THEN** the system shows a list of matching symbols with `exchange`, `code`, and `name`

#### Scenario: Search has no results
- **WHEN** the user searches with a query that matches no symbols
- **THEN** the system shows an empty state indicating no matches

### Requirement: Search results are clearly identifiable
The system SHALL display each search result with an exchange badge and a unique identifier composed of `exchange + code`.

#### Scenario: Result item shows exchange and code
- **WHEN** the system renders a search result item
- **THEN** the item shows the exchange badge and the code next to the name

### Requirement: User can add a searched symbol to watchlist
The system SHALL allow the user to add a symbol from search results to the watchlist.

#### Scenario: Add from search succeeds
- **WHEN** the user selects "Add" on a search result symbol
- **THEN** the symbol is added to the watchlist and the UI reflects the updated state

### Requirement: Search failure is user-visible
If the search operation fails, the system MUST inform the user and allow retry.

#### Scenario: Network error during search
- **WHEN** the symbol search fails due to a network error
- **THEN** the UI shows an error message and a retry action

