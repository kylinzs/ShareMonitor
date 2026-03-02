## ADDED Requirements

### Requirement: User can choose quote data source
The system SHALL provide a setting to choose the quote data source from multiple sources, with a default selection. The default data source MUST be "东方财富".

#### Scenario: Default data source is selected
- **WHEN** the user opens settings for the first time
- **THEN** the system shows the default data source selection as "东方财富"

#### Scenario: Changing data source persists
- **WHEN** the user changes the data source selection
- **THEN** the system persists the selection and uses it for subsequent quote requests

#### Scenario: User can choose between Eastmoney and Tencent
- **WHEN** the user opens the data source selection
- **THEN** the UI shows at least "东方财富" and "腾讯" as selectable options when both are supported

### Requirement: Data source switching applies across features
The system MUST apply the selected quote provider to watchlist quotes and stock detail (quote + history). The "行情" (indices/sectors) screen MUST always use Eastmoney as its data source in MVP.

#### Scenario: Switching data source updates subsequent loads
- **WHEN** the user switches the data source and then navigates to another screen
- **THEN** subsequent data loads use the newly selected data source

### Requirement: Only supported data sources are selectable
The system SHALL only present quote providers as selectable options if they support the MVP quote feature set: watchlist quotes and stock detail history (Intraday + DailyK).

#### Scenario: Unsupported source is not shown
- **WHEN** a data source is missing required capabilities
- **THEN** the settings screen does not show it as a selectable option

### Requirement: User can control auto-refresh behavior
The system SHALL provide a setting to enable or disable auto-refresh, and SHALL allow configuring an auto-refresh interval from a predefined set. The default interval MUST be 15 minutes.

#### Scenario: Auto-refresh disabled
- **WHEN** the user disables auto-refresh
- **THEN** the system does not perform periodic quote fetches without explicit user action

#### Scenario: Auto-refresh interval applied
- **WHEN** the user enables auto-refresh and selects an interval
- **THEN** the system refreshes quotes no more frequently than the selected interval while the watchlist is visible

### Requirement: Settings include data disclaimer
The system MUST provide a data disclaimer and MUST show the currently selected data source name.

#### Scenario: Disclaimer is visible
- **WHEN** the user opens the settings screen
- **THEN** the UI shows a disclaimer and the selected data source name
