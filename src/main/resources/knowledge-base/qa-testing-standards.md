# QA Testing Standards

## General Principles

- Every feature must have traceability from acceptance criteria to test cases.
- Test cases propose verification steps only; they do not certify release readiness.
- Prioritize tests based on business impact and risk.

## Authentication Testing

- Verify valid credentials grant access.
- Verify invalid credentials are rejected with appropriate error messages.
- Test session expiry and token refresh flows.
- Test password reset flows including email delivery, link expiry, and token validation.
- Verify password policy enforcement (length, complexity, history).
- Test account lockout after repeated failed attempts.

## Security Testing

- Validate JWT/token expiry handling.
- Test unauthorized access to protected endpoints.
- Verify input sanitization against injection attacks.
- Ensure sensitive data is not exposed in responses or logs.
- Test CSRF protection where applicable.

## Boundary Testing

- Test empty inputs and null values.
- Test maximum field lengths.
- Test minimum valid values.
- Test values at boundary limits (e.g., token expiry at exactly 30 minutes).

## API Testing Standards

- Verify correct HTTP status codes for success and error scenarios.
- Validate request/response payload schemas.
- Test idempotency for safe retry operations.
- Verify rate limiting behavior if applicable.

## Email Service Testing

- Verify email is sent on trigger events.
- Test email content includes required links and instructions.
- Handle SMTP failures gracefully with user-friendly errors.
- Verify link expiry behavior.
