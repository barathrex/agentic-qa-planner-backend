# QA Guidelines

## Purpose

This document provides guidelines for generating proposed QA plans. All outputs are recommendations for developer review only.

## Test Case Structure

Every proposed test case must include:

1. **Test ID** – Unique identifier (e.g., TC-001)
2. **Title** – Clear, descriptive name
3. **Category** – Unit, API, Integration, E2E, Playwright, Manual, Edge Case, Permission, Failure State, or Regression
4. **Preconditions** – Required system state before execution
5. **Test Steps** – Numbered, actionable steps
6. **Expected Result** – Observable outcome
7. **Priority** – High, Medium, or Low
8. **Reason** – Why this test is important

## Acceptance Criteria Mapping

- Each test case should map to one or more acceptance criteria.
- Coverage is calculated as: (Covered AC / Total AC) × 100%.
- Uncovered criteria must be highlighted for the developer.

## Categories to Consider

Generate only relevant categories based on the feature:

- **Unit Tests** – Isolated component logic
- **API Tests** – Endpoint behavior and contracts
- **Integration Tests** – Component interactions
- **End-to-End Tests** – Full user journeys
- **Playwright Tests** – Browser automation scenarios
- **Manual Tests** – Exploratory or visual verification

## Special Scenario Types

- **Edge Cases** – Boundary values, empty inputs, expired tokens
- **Permission Cases** – Unauthorized access, invalid roles
- **Failure States** – Timeouts, service unavailability
- **Regression Areas** – Existing features that may be affected

## Assumptions

When implementation details are missing, document assumptions explicitly. Example: "Reset token expiry is assumed to be 30 minutes based on acceptance criteria."

## Important Disclaimer

The QA Planning Assistant proposes test cases only. It never determines pass/fail status or release readiness. Final approval always belongs to the developer.
