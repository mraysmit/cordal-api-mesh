# Copilot Instructions for CORDAL

## Banned Dependencies and Practices

- **Mockito is banned.** Do not add mockito-core or any Mockito artifact.
- **Mocking, stubbing, and faking are all banned.** Do not write hand-rolled stubs, fakes, or test doubles of any kind. Tests must use real objects and real integrations.
- No mocking frameworks of any kind (JMockit, EasyMock, PowerMock, etc.).

## Testing

- Tests use JUnit 5, AssertJ, and Testcontainers.
- Tests operate against real instances — real databases, real configuration loading, real HTTP endpoints via Javalin TestTools.
- Integration tests are the primary testing strategy.

## Code Style

- Prefer deleting obsolete classes directly over adding deprecation annotations.
- Ask before changing lifecycle or API status if intent is ambiguous.
- Avoid slangy, trendy, or performative wording; prefer plain, direct language.

## Technology Stack

- Java 25 with --enable-preview
- Maven multi-module project
- JPMS (Java Module System) with module-info.java
- Google Guice 7 for dependency injection
- Jakarta Inject annotations
- Javalin for HTTP
- Virtual threads for concurrency
