## ADDED Requirements

### Requirement: User can open a stock detail view
The system SHALL allow the user to open a detail view for a symbol from the watchlist or search results.

#### Scenario: Open details from watchlist
- **WHEN** the user taps a symbol in the watchlist
- **THEN** the system navigates to the detail view for that symbol

### Requirement: Detail view shows core quote fields
The detail view SHALL display core quote fields including `lastPrice`, `change`, `changePct`, `open`, `high`, `low`, `prevClose`, `volume`, and `quoteTime` when available.

#### Scenario: Detail view renders quote data
- **WHEN** quote data is available for the selected symbol
- **THEN** the detail view displays the quote fields and the last updated time

### Requirement: Detail view supports historical trend display
The system SHALL display historical trends for the selected symbol using time series points, including an intraday line (current trading day) and a daily K-line history.

#### Scenario: History loads and displays
- **WHEN** the detail view requests historical data for the selected time range
- **THEN** the system renders a trend visualization from the returned time series points

### Requirement: Daily K-line history covers recent 6 months
The system MUST provide daily K-line history covering at least the most recent 6 months for the selected symbol (subject to data source availability).

#### Scenario: Daily K-line range meets minimum length
- **WHEN** the user selects the Daily K-line view on the detail screen
- **THEN** the system loads daily history and displays at least the most recent 6 months of data when available

### Requirement: Intraday line shows current trading day
The system MUST provide an intraday line series for the current trading day for the selected symbol (subject to data source availability).

#### Scenario: Intraday line loads for today
- **WHEN** the user selects the Intraday view on the detail screen
- **THEN** the system loads today's intraday time series and renders it as a line trend

### Requirement: Detail refresh is supported
The system SHALL allow the user to refresh the detail view and SHALL update both quote and historical data.

#### Scenario: Refresh updates details
- **WHEN** the user triggers refresh on the detail view
- **THEN** the system fetches latest quote and history and updates the display

### Requirement: Detail view can manage watchlist membership
The system SHALL allow the user to add the current symbol to the watchlist or remove it if already present.

#### Scenario: Toggle watchlist membership
- **WHEN** the user taps the watchlist toggle button on the detail view
- **THEN** the symbol is added to or removed from the watchlist accordingly
