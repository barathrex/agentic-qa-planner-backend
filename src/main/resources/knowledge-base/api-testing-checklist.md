# API Testing Checklist

## Request Validation

- [ ] Valid payloads return expected success responses
- [ ] Missing required fields return 400 Bad Request
- [ ] Invalid field types are rejected
- [ ] Extra/unknown fields are handled appropriately

## HTTP Status Codes

- [ ] 200/201 for successful operations
- [ ] 400 for client validation errors
- [ ] 401 for missing/invalid authentication
- [ ] 403 for insufficient permissions
- [ ] 404 for non-existent resources
- [ ] 409 for conflict scenarios
- [ ] 500 for server errors (with safe error messages).

## Authentication & Authorization

- [ ] Endpoints require authentication where expected
- [ ] Invalid tokens are rejected
- [ ] Expired tokens return appropriate errors
- [ ] Role-based access is enforced

## Response Schema

- [ ] Response body matches documented schema
- [ ] Error responses include meaningful messages
- [ ] Sensitive fields are not exposed

## Edge Cases

- [ ] Empty request body handling
- [ ] Maximum payload size handling
- [ ] Special characters in input fields
- [ ] Duplicate request handling

## Failure States

- [ ] API timeout behavior
- [ ] Database connection failure handling
- [ ] External service (email, payment) failure handling
- [ ] Graceful degradation when dependencies are unavailable

## Integration

- [ ] End-to-end flow through multiple API calls
- [ ] State consistency across related endpoints
- [ ] Event/webhook triggers fire correctly
