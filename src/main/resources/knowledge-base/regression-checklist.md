# Regression Checklist

## Authentication & Session

- [ ] Existing login flow still works after changes
- [ ] User registration is unaffected
- [ ] Session management and logout behave correctly
- [ ] Token refresh mechanisms remain functional

## User Profile

- [ ] Profile update functionality is unaffected
- [ ] Password change flow works independently of reset flow
- [ ] User preferences persist correctly

## Core Navigation

- [ ] Main application routes remain accessible
- [ ] Protected routes still enforce authentication
- [ ] Redirect behavior after login/logout is correct

## Data Integrity

- [ ] No unintended data loss during new feature operations
- [ ] Database constraints are respected
- [ ] Concurrent access scenarios do not corrupt state

## Error Handling

- [ ] Global error handlers still function
- [ ] User-facing error messages remain helpful
- [ ] Logging captures sufficient detail for debugging

## Performance

- [ ] No significant degradation in response times
- [ ] New endpoints do not block existing critical paths
