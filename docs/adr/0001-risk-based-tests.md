# ADR 0001: Risk-Based Contract Tests

## Status

Accepted on 2026-07-09.

## Decision

SparkAssist commits focused JUnit tests for pure config, input, sound routing,
catalog ownership, and safety exclusions. Minecraft client and mixin behavior is
verified through the smallest callable rule Interface plus build and metadata
checks; source-string tests are a last resort.

Production code must not expose reset hooks or alternate behavior solely for
tests. `check` runs both `test` and `verifyArchitecture`.

## Consequences

Contract regressions fail locally before jar packaging. Client-only integration
still requires build or runtime verification where plain JUnit cannot instantiate
Minecraft objects.
